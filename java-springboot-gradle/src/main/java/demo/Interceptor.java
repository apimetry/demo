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
            HandlerMethod method = (HandlerMethod) handler;
            if (method.getMethodAnnotation(NoAuth.class) != null) {
                return true;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        final Merchant merchant = this.database.findMerchantByAuthToken(auth)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        request.setAttribute("Merchant-ID", merchant.id());
        final Span span = Span.current();
        if (span != null) {
            span.setAttribute("apimetry.customer.id", merchant.id());
            span.setAttribute("apimetry.customer.name", merchant.name());
            if (request instanceof CachingRequestBodyFilter.CachedRequest wrapper) {
                span.setAttribute("http.body", wrapper.getInputStream().toString());
            }
        }
        return true;
    }
}
