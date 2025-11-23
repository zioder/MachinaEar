package MachinaEar.iam.security;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Security headers filter
 * Adds security-related HTTP headers to all responses
 *
 * Headers added:
 * - HSTS (Strict-Transport-Security) - Force HTTPS
 * - X-Content-Type-Options - Prevent MIME sniffing
 * - X-Frame-Options - Prevent clickjacking
 * - X-XSS-Protection - Enable browser XSS protection
 * - Content-Security-Policy - Restrict resource loading
 * - Referrer-Policy - Control referrer information
 */
@WebFilter(urlPatterns = "/*")
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // HSTS - Force HTTPS for 1 year, include subdomains
        // Only enable in production (when using HTTPS)
        String protocol = request.getScheme();
        if ("https".equals(protocol)) {
            httpResponse.setHeader("Strict-Transport-Security",
                "max-age=31536000; includeSubDomains; preload");
        }

        // Prevent MIME type sniffing
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // Prevent clickjacking - don't allow embedding in iframes
        httpResponse.setHeader("X-Frame-Options", "DENY");

        // Enable browser XSS filter
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

        // Content Security Policy - restrict resource loading
        // Adjust as needed for your frontend requirements
        httpResponse.setHeader("Content-Security-Policy",
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' data:; " +
            "connect-src 'self' http://localhost:3000 http://localhost:8080; " +
            "frame-ancestors 'none';"
        );

        // Control referrer information leakage
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions Policy (formerly Feature-Policy)
        // Disable potentially dangerous browser features
        httpResponse.setHeader("Permissions-Policy",
            "geolocation=(), microphone=(), camera=(), payment=(), usb=()");

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void destroy() {}
}
