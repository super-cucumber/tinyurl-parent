package com.vipgp.tinyurl.dubbo.provider.id.generator;

import com.vipgp.tinyurl.dubbo.provider.id.generator.buffer.BufferLeafSegment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/1/14
 */
@Component
//@Conditional(LeafSegmentCondition.class)
@ConditionalOnProperty(value = Constant.ID_GENERATOR_PROVIDER_KEY,havingValue = Constant.ID_GENERATOR_PROVIDER_LEAF_SEGMENT_LOCAL)
public class BufferLeafSegmentProcessor implements Generatable {

    @Autowired
    BufferLeafSegment bufferLeafSegment;

    /**
     * generate sequence id
     *
     * @return
     */
    @Override
    public Long generate(String tag) {
        return bufferLeafSegment.getId(tag);
    }
}
