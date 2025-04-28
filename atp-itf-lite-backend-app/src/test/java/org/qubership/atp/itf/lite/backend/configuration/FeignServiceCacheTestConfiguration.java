package org.qubership.atp.itf.lite.backend.configuration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({"org.qubership.atp.itf.lite.backend.feign"})
@EnableCaching
public class FeignServiceCacheTestConfiguration {

}
