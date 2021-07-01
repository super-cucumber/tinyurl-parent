package com.vipgp.tinyurl.dubbo.provider.service.handler.precreate;

import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/22 15:40
 */
public abstract class AbstractCreateValidation implements CreateValidation {

    /**
     * the next in the chain
     */
    private CreateValidation next;

    /**
     * process the handler
     * @param baseUrl
     * @param rawUrl
     * @param aliasCode
     * @return
     */
    @Override
    public BaseResult process(String baseUrl, String rawUrl,String aliasCode){
        BaseResult result= doProcess(baseUrl, rawUrl, aliasCode);
        if(result!=null){
            return result;
        }

        return processNext(baseUrl, rawUrl, aliasCode);
    }

    /**
     * do process in each validation
     * @param baseUrl
     * @param rawUrl
     * @param aliasCode
     * @return
     */
    public abstract BaseResult doProcess(String baseUrl, String rawUrl,String aliasCode);

    /**
     * set next to chain
     *
     * @param handler
     */
    @Override
    public void setNext(CreateValidation handler) {
        this.next=handler;
    }

    /**
     * process the next in the chain
     * @param baseUrl
     * @param rawUrl
     * @param aliasCode
     * @return
     */
    @Override
    public BaseResult processNext(String baseUrl, String rawUrl, String aliasCode) {
        if(this.next!=null){
            return this.next.process(baseUrl, rawUrl, aliasCode);
        }

        return null;
    }
}
