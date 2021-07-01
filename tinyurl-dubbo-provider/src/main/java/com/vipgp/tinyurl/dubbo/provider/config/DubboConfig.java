package com.vipgp.tinyurl.dubbo.provider.config;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/1/28 9:38
 */
@Configuration
public class DubboConfig {

    @NacosValue(value = "${dubbo.application.name}", autoRefreshed = true)
    String applicationName;
    @NacosValue(value = "${dubbo.registry.protocol}", autoRefreshed = true)
    String registryProtocol;
    @NacosValue(value = "${dubbo.registry.address}", autoRefreshed = true)
    String registryAddress;
    @NacosValue(value = "${dubbo.protocol.name}", autoRefreshed = true)
    String protocolName;
    @NacosValue(value = "${dubbo.protocol.port}", autoRefreshed = true)
    int protocolPort;
    @NacosValue(value = "${dubbo.registry.file}", autoRefreshed = true)
    String registryFile;
    @NacosValue(value = "${dubbo.protocol.threads}", autoRefreshed = true)
    Integer protocolThreads;
    @NacosValue(value = "${dubbo.protocol.threadpool}", autoRefreshed = true)
    String protocolThreadpool;
    @NacosValue(value = "${dubbo.provider.delay}", autoRefreshed = true)
    Integer providerDelay;
    @NacosValue(value = "${dubbo.provider.timeout}", autoRefreshed = true)
    Integer providerTimeout;
    @NacosValue(value = "${dubbo.provider.retries}", autoRefreshed = true)
    Integer providerRetries;

    @Bean
    public ApplicationConfig applicationConfig(){
        ApplicationConfig applicationConfig=new ApplicationConfig();
        applicationConfig.setName(applicationName);
        return applicationConfig;
    }

    @Bean
    public RegistryConfig registryConfig(){
        RegistryConfig registryConfig=new RegistryConfig();
        registryConfig.setProtocol(registryProtocol);
        registryConfig.setAddress(registryAddress);
        registryConfig.setFile(registryFile);

        return registryConfig;
    }

    @Bean
    public ProtocolConfig protocolConfig(){
        ProtocolConfig protocolConfig=new ProtocolConfig();
        protocolConfig.setName(protocolName);
        protocolConfig.setPort(protocolPort);
        protocolConfig.setThreads(protocolThreads);
        protocolConfig.setThreadpool(protocolThreadpool);

        return protocolConfig;
    }

    @Bean
    public ProviderConfig providerConfig(){
        ProviderConfig providerConfig=new ProviderConfig();
        providerConfig.setDelay(providerDelay);
        providerConfig.setTimeout(providerTimeout);
        providerConfig.setRetries(providerRetries);
        return providerConfig;
    }
}
