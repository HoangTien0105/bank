    package com.bank;

    import com.github.benmanes.caffeine.cache.Caffeine;
    import org.springframework.cache.CacheManager;
    import org.springframework.cache.annotation.EnableCaching;
    import org.springframework.cache.caffeine.CaffeineCacheManager;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;

    import java.util.concurrent.TimeUnit;

    @EnableCaching
    @Configuration
    public class CacheConfig {

        @Bean
        public Caffeine<Object, Object> caffeineConfig(){
            return Caffeine.newBuilder()
                    .expireAfterWrite(60, TimeUnit.MINUTES) //Mỗi item sẽ hết hạn sau 60p
                    .initialCapacity(100) //Tạo sẵn 100 slot
                    .maximumSize(500); //Tối đa 500
        }

        @Bean
        public CacheManager cacheManager(Caffeine caffeine){
            CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
            caffeineCacheManager.setCaffeine(caffeine);
            return  caffeineCacheManager;
        }
    }
