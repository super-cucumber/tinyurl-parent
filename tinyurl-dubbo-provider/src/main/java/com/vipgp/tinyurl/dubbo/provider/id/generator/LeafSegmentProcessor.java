package com.vipgp.tinyurl.dubbo.provider.id.generator;

import com.vipgp.doraemon.service.dubbo.IDGenerateService;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/1/14
 */
@Component
//@Conditional(LeafSegmentCondition.class)
@ConditionalOnProperty(value = Constant.ID_GENERATOR_PROVIDER_KEY,havingValue = Constant.ID_GENERATOR_PROVIDER_LEAF_SEGMENT)
public class LeafSegmentProcessor implements Generatable {

    @Reference(version = "1.0.0")
    IDGenerateService idGenerateService;

    /**
     * generate sequence id
     *
     * @return
     */
    @Override
    public Long generate(String tag) {
        return idGenerateService.getLeafSegmentId(tag);
    }
}
