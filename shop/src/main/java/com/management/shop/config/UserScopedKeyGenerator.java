package com.management.shop.config;

import ch.qos.logback.core.net.SyslogOutputStream;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.lang.reflect.Method;
import java.util.StringJoiner;

@Component("userScopedKeyGenerator")
public class UserScopedKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        // Get username from Spring Security
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null) ? auth.getName() : "anonymous";
        String cachePrefix = "default";
        if (method.isAnnotationPresent(Cacheable.class)) {
            Cacheable cacheable = method.getAnnotation(Cacheable.class);
            if (cacheable.value().length > 0) {
                cachePrefix = cacheable.value()[0];  // e.g., "sales", "customers"
            }
        }
        // Join all method params into one key
        StringJoiner joiner = new StringJoiner("_");

        for (Object param : params) {
            if (param != null) {

                joiner.add(param.toString());
            } else {
                System.out.println("The params are --> null");
                joiner.add("null");
            }
        }

        // sales::username::methodName::params
        return cachePrefix + "::" + username + "::" + method.getName() + "::" + joiner;
    }
}