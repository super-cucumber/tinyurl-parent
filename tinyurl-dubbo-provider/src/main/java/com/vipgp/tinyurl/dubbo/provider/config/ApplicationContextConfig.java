package com.vipgp.tinyurl.dubbo.provider.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/2/3 0:11
 */
@Slf4j
@Configuration
public class ApplicationContextConfig implements ApplicationContextInitializer {
    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      log.info("initialize start");
    }
}
