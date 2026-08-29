package com.management.shop.filter;

import com.management.shop.security.UserInfoUserDetailsService;
import com.management.shop.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserInfoUserDetailsService userDetailsService;

    @Value("${auth.cookie.name:jwt}")
    private String authCookieName;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;
        String username = null;
        // 1. Try to get token from Authorization Header first (Standard approach)
        String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        // 2. If Header is empty, try to get token from Cookies (Fallback)
        if (token == null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (authCookieName.equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        System.out.println("JwtAuthFilter: Checking for JWT token in request..."+token);


       /* if(token ==null){
            token="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqdW5haWQxIiwiaWF0IjoxNzcxMTY1MzgyLCJleHAiOjE3NzExODMzODJ9.jzdxedia4vMH4pgmNAOS6l-jrmN64fnkvJkQmgGbEdM";
        }*/

        // 3. If a token was found, validate it
        if (token != null) {
            try {
                username = jwtService.extractUsername(token);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (jwtService.validateToken(token, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (Exception e) {
                // Log token errors (expired, malformed) but do not throw exception.
                // This allows the request to proceed (anonymously) so 401/403 is handled by SecurityConfig
                log.error("Could not set user authentication in security context", e);
            }
        }

        // 4. ALWAYS continue the filter chain
        // If authentication failed, the user is anonymous.
        // Spring Security Config will decide if they are allowed to access the endpoint.
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip the JWT filter entirely for actuator endpoints
        return path.startsWith("/actuator");
    }
}
