# Java / Spring Boot - Integration Demo
The following guide is a shortened and minimal summary of the Spring Boot project in this directory. The actual code in this
directory will contain more details than what is written here.

## Step 1 - Add Open Telemetry SDK
Open `build.gradle` and add the following
```groovy
dependencyManagement {
    imports {
        mavenBom 'io.opentelemetry:opentelemetry-bom:1.40.0'
    }
}

dependencies {
    implementation 'io.opentelemetry:opentelemetry-api'
}
```

## Step 2 - Create or Reuse an Request Interceptor to Add Customer Attributes
In this case we will create a new interceptor that loads a merchant (customer) and add the attribute
```java
public class Interceptor implements HandlerInterceptor {

    private final Database database;

    public Interceptor(Database database) {
        this.database = database;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        final String auth = request.getHeader("Authorization");
       
        final Merchant merchant = this.database.findMerchantByAuthToken(auth)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        
        final Span span = Span.current();
        if (span != null) {
            span.setAttribute("apimetry.customer.id", merchant.id());
            span.setAttribute("apimetry.customer.name", merchant.name());
        }
        return true;
    }
}
```

## Step 3 - Update the WebMvcConfigurer to Register the Interceptor
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new Interceptor());
    }
}
```

## Step 4 - Add the Open Telemetry Agent when Running the Application
Assuming you run the application as a JAR file in a Dockerfile, add the following steps in the Dockerfile
```Dockerfile
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.10.0/opentelemetry-javaagent.jar /optel-agent.jar
RUN chmod a+r /optel-agent.jar
```
Then attach the agent to your application via the command line arguments
```
java -javaagent:/optel-agent.jar -jar /your/app.jar
```

## Step 5 - Add HTTP Body to the Span
Unfortunately by default the body of HTTP request will not be included in the telemetry data. Using Spring Boot
there are multiple ways you can go about doing achieving this, for this demonstration we will create a request filter
which adds support to reading the request body multiple times by caching the data.
```java
@Component
public class CachingRequestBodyFilter extends GenericFilterBean {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        chain.doFilter(new CacheRequest(httpRequest), response);
    }

    static class CacheRequest extends HttpServletRequestWrapper {

        private byte[] cache;

        public CacheRequest(HttpServletRequest servlet) {
            super(servlet);
            this.cache = null;
        }
        
        public CachedInputStream getAndCacheInputStream() throws IOException {
            if (cache != null) {
                return new CachedInputStream(new ByteArrayInputStream(this.cache));
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[16384];

            while ((nRead = super.getInputStream().read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            this.cache = buffer.toByteArray();
            return new CachedInputStream(new ByteArrayInputStream(this.cache));
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (cache != null) {
                return new CachedInputStream(new ByteArrayInputStream(this.cache));
            }
            return super.getInputStream();
        }
    }

    static class CachedInputStream extends ServletInputStream {

        private final ByteArrayInputStream source;

        public CachedInputStream(ByteArrayInputStream source) {
            this.source = source;
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
        }

        @Override
        public int read() throws IOException {
            return this.source.read();
        }

        @Override
        public String toString() {
            return new String(this.source.readAllBytes());
        }
    }
}
```
Then in our interceptor we read the HTTP body and add it as an attribute
```java
if (span != null) {
    span.setAttribute("apimetry.customer.id", merchant.id());
    span.setAttribute("apimetry.customer.name", merchant.name());
    if (request instanceof CachingRequestBodyFilter.CacheRequest wrapper) {
        span.setAttribute("http.body", wrapper.getAndCacheInputStream().toString());
    }
}
```
## (Optional) Step 6 - Support an Ignore Apimetry
It will be a common case that particular endpoints should not be integrated with Apimetry, or that it is not relevant
to send the HTTP body (for example the body is an image or non JSON).

To achieve this we will two annotations that we can add to our endpoints
- `@ApimetryIgnore` If the entire endpoint usage should be excluded from Apimetry
- `@ApimetryIgnoreBody` If we should not add the body as an attribute

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ApimetryIgnore {
}
```

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ApimetryIgnoreBody {
}
```

Now in our interceptor we need to add support for these annotations
```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    final String auth = request.getHeader("Authorization");

    final Merchant merchant = this.database.findMerchantByAuthToken(auth)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));

    final boolean ignore = (handler instanceof HandlerMethod method)
        ? method.getMethodAnnotation(ApimetryIgnore.class) != null
        : false;

    if (ignore) {
        return true;
    }
    final Span span = Span.current();
    if (span != null) {
        span.setAttribute("apimetry.customer.id", merchant.id());
        span.setAttribute("apimetry.customer.name", merchant.name());
        final boolean includeBody = (handler instanceof HandlerMethod method)
            ? method.getMethodAnnotation(ApimetryIgnoreBody.class) == null
            : true;
        if (includeBody && request instanceof CachingRequestBodyFilter.CacheRequest wrapper) {
            span.setAttribute("http.body", wrapper.getAndCacheInputStream().toString());
        }
    }
    return true;
}
```

Then we can use these annotations on the methods to control what endpoints are monitored by Apimetry
```java
@GetMapping("/foo")
@ApimetryIgnore
public FooResponse foo() {
    
}

@PostMapping("/bar")
@ApimetryIgnoreBody
public BarResponse bar(@RequestBody BarRequest request) {
    
}
```
