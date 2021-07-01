package com.vipgp.tinyurl.dubbo.provider.service.handler.precreate;

import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/24 15:28
 */
public abstract class AbstractPreCreateProcessor implements PreCreateProcessor{

    /**
     * validation chain init
     */
    protected abstract void initChain();

    /**
     * the header of the chain
     * @return
     */
    protected abstract CreateValidation getHeader();

    /**
     * process pre create
     * @param baseUrl
     * @param rawUrl
     * @param aliasCode
     * @return
     */
    @Override
    public BaseResult process(String baseUrl, String rawUrl,String aliasCode) {
        return this.getHeader().process(baseUrl, rawUrl, aliasCode);
    }
}
