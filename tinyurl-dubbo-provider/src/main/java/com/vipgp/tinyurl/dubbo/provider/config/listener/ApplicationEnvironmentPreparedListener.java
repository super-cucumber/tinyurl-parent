package com.vipgp.tinyurl.dubbo.provider.config.listener;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import org.slf4j.MDC;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/6/22 20:38
 */
@Component
public class ApplicationEnvironmentPreparedListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @NacosValue(value = "${logging.file.path}",autoRefreshed = true)
    private String loggingFilePath;

    /**
     * Handle an application event.
     *
     * @param event the event to respond to
     */
    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Map<String, String> contextMap = new HashMap<>();
        contextMap.put("logPath", loggingFilePath);
        System.setProperty("logPath",loggingFilePath);

        MDC.setContextMap(contextMap);
    }
}
