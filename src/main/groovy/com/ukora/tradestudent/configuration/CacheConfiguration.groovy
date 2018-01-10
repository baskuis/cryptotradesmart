package com.ukora.tradestudent.configuration

import com.google.common.cache.CacheBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.cache.interceptor.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import java.util.concurrent.TimeUnit

@EnableCaching
@Configuration
class CacheConfiguration implements CachingConfigurer {

    static final long MAX_CACHED_ENTRIES = 10000

    @Bean
    @Override
    CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager() {
            @Override
            protected Cache createConcurrentMapCache(final String name) {
                return new ConcurrentMapCache(
                        name,
                        CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).maximumSize(MAX_CACHED_ENTRIES).build().asMap(),
                        true
                )
            }
        }
        return cacheManager
    }

    @Autowired CacheManager cacheManager

    @Bean
    @Override
    KeyGenerator keyGenerator() {
        return new SimpleKeyGenerator()
    }

    @Bean
    @Override
    CacheResolver cacheResolver() {
        return new SimpleCacheResolver(cacheManager)
    }

    @Bean
    @Override
    CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler()
    }

}