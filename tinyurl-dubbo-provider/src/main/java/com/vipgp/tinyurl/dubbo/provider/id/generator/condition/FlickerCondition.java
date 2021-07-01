package com.vipgp.tinyurl.dubbo.provider.id.generator.condition;

import com.vipgp.tinyurl.dubbo.provider.id.generator.Constant;
import com.vipgp.tinyurl.dubbo.provider.id.generator.ProviderEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/1/25 14:08
 */
@Slf4j
public class FlickerCondition implements Condition {

    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        Environment env= conditionContext.getEnvironment();
        String propertyValue= env.getProperty(Constant.ID_GENERATOR_PROVIDER_KEY);
        log.info("propertyValue="+propertyValue);
        if(ProviderEnum.FLIKER.getKey().equals(propertyValue)){
            return true;
        }

        return false;
    }
}
