package com.duewise.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Turns on Spring's cache annotations. With no cache library on the classpath, Spring Boot uses a
 * simple in-memory map, which is enough for small reference lists such as currencies. Swapping in
 * Redis or Caffeine later needs no code changes in the services.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
