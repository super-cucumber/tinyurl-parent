package com.vipgp.tinyurl.dubbo.provider.service.handler.prequery;

import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/22 15:40
 */
public abstract class AbstractQueryValidation implements QueryValidation {

    /**
     * the next in the chain
     */
    private QueryValidation next;

    /**
     * process the handler
     *
     * @param baseUrl
     * @param aliasCode
     * @return
     */
    @Override
    public abstract BaseResult process(String baseUrl, String aliasCode);

    /**
     * set next to chain
     *
     * @param handler
     */
    @Override
    public void setNext(QueryValidation handler) {
        this.next=handler;
    }

    /**
     * process the next in the chain
     *
     * @param baseUrl
     * @param aliasCode
     * @return
     */
    @Override
    public BaseResult processNext(String baseUrl, String aliasCode) {
        if(this.next!=null){
           return this.next.process(baseUrl, aliasCode);
        }

        return null;
    }
}
