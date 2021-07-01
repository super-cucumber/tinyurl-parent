package com.vipgp.tinyurl.dubbo.provider.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/5/18 13:20
 */
@Configuration
public class LuaConfig {

    @Bean("addtinyurlscript")
    public ResourceScriptSource addTinyUrlScriptSource(){
        ResourceScriptSource scriptSource=new ResourceScriptSource(new ClassPathResource("scripts/addtinyurl.lua"));
        return scriptSource;
    }

    @Bean("rollbacktinyurlscript")
    public ResourceScriptSource rollbackTinyUrlScriptSource(){
        ResourceScriptSource scriptSource=new ResourceScriptSource(new ClassPathResource("scripts/rollbacktinyurl.lua"));
        return scriptSource;
    }

    @Bean("unlockscript")
    public ResourceScriptSource unlockScriptSource(){
        ResourceScriptSource scriptSource=new ResourceScriptSource(new ClassPathResource("scripts/unlock.lua"));
        return scriptSource;
    }
}
