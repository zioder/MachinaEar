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
        // Check X-Forwarded-Proto for Nginx compatibility
        String forwardedProto = ((jakarta.servlet.http.HttpServletRequest)request).getHeader("X-Forwarded-Proto");
        String protocol = forwardedProto != null ? forwardedProto : request.getScheme();
        
        if ("https".equalsIgnoreCase(protocol)) {
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
        // Updated to include production domains, Vercel, and Vercel Live
        // Added worker-src with blob: for ALTCHA widget Web Workers
        httpResponse.setHeader("Content-Security-Policy",
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://vercel.live; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' data:; " +
            "connect-src 'self' https://localhost:3000 https://localhost:8443 https://www.machinaear.me https://machinaear.me https://*.vercel.app https://vercel.live; " +
            "frame-src 'self' https://vercel.live; " +
            "child-src 'self' blob: https://vercel.live; " +
            "worker-src 'self' blob:; " +
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
