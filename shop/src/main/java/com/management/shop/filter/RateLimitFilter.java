package com.management.shop.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

   private final Map<String, Bucket> bucket=new ConcurrentHashMap<>();

   private final static String JWT_SECRET="5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";
   private final static String JWT_COOKIE_NAME="jwt";

   private Bucket resolveBucket(String userId){
       return bucket.computeIfAbsent(userId, k->{
           Refill refill = Refill.greedy(10, Duration.ofMinutes(1));
           Bandwidth limit= Bandwidth.classic(10, refill);
           return Bucket4j.builder().addLimit(limit).build();
       });
   }

   @Override
   protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException{

     String uri=request.getRequestURI();
     if(!uri.startsWith("/apil/")){
         filterChain.doFilter(request,response);
         return;
     }

     String userId=extractUserIdFromJwtCookie(request);

       // Fallback to IP address if JWT is missing or invalid
       if (userId == null) {
           userId = request.getRemoteAddr();
       }

       Bucket bucket = resolveBucket(userId);

       if (bucket.tryConsume(1)) {
           filterChain.doFilter(request, response);
       } else {
           response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
           response.getWriter().write("Rate limit exceeded");
       }

   }

   private String extractUserIdFromJwtCookie(HttpServletRequest request){
       Cookie[] cookies=request.getCookies();
       if(cookies==null || cookies.length==0){return null;}

       for(Cookie cookie:cookies){
           if(JWT_COOKIE_NAME.equals(cookie.getName())){
               String token=cookie.getValue();
               try{
                   Claims claims= Jwts.parser()
                           .setSigningKey(JWT_SECRET.getBytes())
                           .parseClaimsJws(token).getBody();

                   return claims.get("userId", String.class);
               }
               catch (JwtException e){}
           }
       }

       return null;
   }
}
