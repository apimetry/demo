package demo;

import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

public class Interceptor implements HandlerInterceptor {

    private final Database database;

    public Interceptor() {
        this.database = Database.INSTANCE;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        final String auth = request.getHeader("Authorization");
        if (auth == null || auth.isBlank()) {
            if (handler instanceof HandlerMethod method) {
                if (method.getMethodAnnotation(NoAuth.class) != null) {
                    return true;
                }
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        final Merchant merchant = this.database.findMerchantByAuthToken(auth)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        request.setAttribute("Merchant-ID", merchant.id());
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
}
