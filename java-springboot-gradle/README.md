# Java / Spring Boot / Gradle Integration Demo

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
        if (auth == null || auth.isBlank()) {
            HandlerMethod method = (HandlerMethod) handler;
            if (method.getMethodAnnotation(NoAuth.class) != null) {
                return true;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        
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
