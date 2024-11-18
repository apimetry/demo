package demo;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class CachingRequestBodyFilter extends GenericFilterBean {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        chain.doFilter(new CachedRequest(httpRequest), response);
    }

    static class CachedRequest extends HttpServletRequestWrapper {

        private byte[] cache;

        public CachedRequest(HttpServletRequest servlet) {
            super(servlet);
            this.cache = null;
        }


        @Override
        public CachedInputStream getInputStream() throws IOException {
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
