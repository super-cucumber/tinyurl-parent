package com.vipgp.tinyurl.dubbo.provider.config;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/1/28 13:08
 */
@Configuration
@Data
@Slf4j
public class IdGeneratorConfig {

    @NacosValue(value = "${id.generator.provider}", autoRefreshed = true)
    private String idGeneratorProvider;

    @NacosValue(value = "${id.generator.url.base}", autoRefreshed = true)
    private String idGeneratorUrlBase;

    public IdGeneratorConfig(){
      log.info("IdGeneratorConfig init");
    }

    @PostConstruct
    public void init(){
        log.info("IdGeneratorConfig post construct init");
    }
}
