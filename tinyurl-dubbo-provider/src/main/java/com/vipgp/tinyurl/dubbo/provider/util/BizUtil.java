package com.vipgp.tinyurl.dubbo.provider.util;

import com.vipgp.tinyurl.dubbo.provider.domain.TinyRawUrlRelDO;
import com.vipgp.tinyurl.dubbo.provider.persistence.TxnLogModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/6/8 22:03
 */
public class BizUtil {

    /**
     * transfer
     * @param source
     * @return
     */
    public static List<TinyRawUrlRelDO> copyFrom(List<TxnLogModel> source){
        List<TinyRawUrlRelDO> target=new ArrayList<>();
        for (TxnLogModel item :
                source) {
            TinyRawUrlRelDO entity = new TinyRawUrlRelDO();
            entity.setId(item.getId());
            entity.setBaseUrl(item.getBaseUrlKey());
            entity.setTinyUrl(item.getAliasCode());
            entity.setRawUrl(item.getRawUrl());
            target.add(entity);
        }

        return target;
    }
}
