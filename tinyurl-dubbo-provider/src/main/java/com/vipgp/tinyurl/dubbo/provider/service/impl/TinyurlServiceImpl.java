package com.vipgp.tinyurl.dubbo.provider.service.impl;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.vipgp.tinyurl.dubbo.provider.exception.BaseAppException;
import com.vipgp.tinyurl.dubbo.provider.id.generator.Generatable;
import com.vipgp.tinyurl.dubbo.provider.manager.LookupManager;
import com.vipgp.tinyurl.dubbo.provider.manager.RedisManager;
import com.vipgp.tinyurl.dubbo.provider.manager.TinyManager;
import com.vipgp.tinyurl.dubbo.provider.service.handler.create.CreateProcessor;
import com.vipgp.tinyurl.dubbo.provider.service.handler.precreate.PreCreateProcessor;
import com.vipgp.tinyurl.dubbo.provider.service.handler.prequery.PreQueryProcessor;
import com.vipgp.tinyurl.dubbo.provider.service.handler.query.QueryProcessor;
import com.vipgp.tinyurl.dubbo.provider.util.CacheUtil;
import com.vipgp.tinyurl.dubbo.provider.util.ErrorCode;
import com.vipgp.tinyurl.dubbo.provider.util.TinyurlValidator;
import com.vipgp.tinyurl.dubbo.service.TinyurlService;
import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * author: Sando
 * date: 2021/1/10
 */
@Slf4j
@Service(version = "1.0.0",interfaceClass = TinyurlService.class)
@Component("TinyurlServiceV1")
public class TinyurlServiceImpl implements TinyurlService {

    @Autowired
    Generatable generator;

    @Autowired
    TinyManager tinyManager;

    @Autowired
    RedisManager redisManager;

    @Autowired
    CacheUtil cacheUtil;

    @Autowired
    LookupManager lookupManager;

    @Autowired
    TinyurlValidator validator;

    @Resource(name="preCreateProcessorWithAlias")
    PreCreateProcessor preCreateProcessorWithAlias;
    @Resource(name="preCreateProcessorNoAlias")
    PreCreateProcessor preCreateProcessorNoAlias;
    @Resource(name="createProcessorNoAlias")
    CreateProcessor createProcessNoAlias;
    @Resource(name="createProcessorWithAlias")
    CreateProcessor createProcessWithAlias;
    @Autowired
    PreQueryProcessor preQueryProcessor;
    @Autowired
    QueryProcessor queryProcessor;

    @NacosValue(value = "${tiny.url.length}",autoRefreshed = true)
    int codeLength;

    /**
     * generate alias code by system
     * @param baseUrl
     * @param rawUrl
     * @return
     */
    @Override
    public BaseResult<String> createTinyurl(String baseUrl, String rawUrl) {
        try {
            long from=System.nanoTime();
            /**
             * Step 0: pre create
             */
            long start=System.nanoTime();
            BaseResult result = preCreateProcessorNoAlias.process(baseUrl, rawUrl, null);
            long end=System.nanoTime();
            log.info("no alias code, pre create validation, cost {}ns {}ms", end-start,(end-start)/1000000);
            if (result != null) {
                return result;
            }

            /**
             * Step 1: create
             */
            start=System.nanoTime();
            String tinyUrl=createProcessNoAlias.doCreate(rawUrl,baseUrl,null);
            end=System.nanoTime();
            log.info("no alias code, do create, cost {}ns {}ms", end-start,(end-start)/1000000);

            /**
             * Step 2: post create
             */
            // TODO
            // async add creation history for premium user

            long to=System.nanoTime();
            result= BaseResult.success(tinyUrl);
            log.info("no alias code, create url, cost {}ns {}ms", to-from,(to-from)/1000000);
            return result;
        }catch (BaseAppException inner){
            log.error("inner exception {}",inner.toString(),inner);
            return BaseResult.fail(ErrorCode.URL_CREATE_EXCEPTION.getCode(),inner.getDesc());
        } catch (Exception ex){
            log.error("create exception",ex);
            return BaseResult.fail(ErrorCode.URL_CREATE_EXCEPTION.getCode(),ex.getMessage());
        }
    }


    /**
     * use the alias code from user inputted
     * @param baseUrl
     * @param aliasCode
     * @param rawUrl
     * @return
     */
    @Override
    public BaseResult<String> createTinyurl(String baseUrl,String aliasCode, String rawUrl) {
        try {
            long start=System.nanoTime();
            /**
             * Step 0: pre create
             */
            // the length of alias code can be less than defined, should pad before query as we insert the padded alias code to cache & db
            String aliasCodePad = StringUtils.isEmpty(aliasCode) ? aliasCode : StringUtils.leftPad(aliasCode, codeLength, '0');
            BaseResult result = preCreateProcessorWithAlias.process(baseUrl, rawUrl, aliasCodePad);
            if (result != null) {
                return result;
            }

            /**
             * Step 1: create
             */
            String tinyUrl = createProcessWithAlias.doCreate(rawUrl, baseUrl, aliasCode);

            /**
             * Step 2: post create
             */
            // TODO
            // async add creation history for premium user
            long end=System.nanoTime();
            result= BaseResult.success(tinyUrl);
            log.info("with alias code, create url, cost {}ns {}ms", end-start,(end-start)/1000000);
            return result;
        } catch (BaseAppException inner) {
            log.error("inner exception {}", inner.toString(), inner);
            return BaseResult.fail(ErrorCode.URL_CREATE_EXCEPTION.getCode(), inner.getDesc());
        } catch (Exception ex) {
            log.error("create exception", ex);
            return BaseResult.fail(ErrorCode.URL_CREATE_EXCEPTION.getCode(), ex.getMessage());
        }
    }

    /**
     * local cache + local lock + random expired time, keep off cache avalanche and hotspot invalid
     * use redis bitmap to check exist or not, keep off cache penetration
     * @param baseUrl
     * @param aliasCode
     * @return
     */
    @Override
    public BaseResult<String> getLongUrl(String baseUrl, String aliasCode) {
        try{
            /**
             * Step 0: pre query
             */
            // the length of alias code can be less than defined, should pad before query as we insert the padded alias code to cache & db
            String aliasCodePad=StringUtils.leftPad(aliasCode,codeLength,'0');
            BaseResult result=preQueryProcessor.process(baseUrl, aliasCodePad);
            if(result!=null){
                return result;
            }

            /**
             * Step 1: query
             */
            String rawUrl= queryProcessor.doQuery(baseUrl, aliasCodePad);
            if(StringUtils.isEmpty(rawUrl)){
                result= BaseResult.fail(ErrorCode.URL_UNEXIST.getCode(), ErrorCode.URL_UNEXIST.getInfo());
            }else {
                result=BaseResult.success(rawUrl);
            }

            /**
             * Step 2: post query
             */
            // TODO

            return result;
        }catch (BaseAppException inner){
            log.error("inner exception {}",inner.toString(),inner);
            return BaseResult.fail(ErrorCode.URL_GET_EXCEPTION.getCode(),inner.getDesc());
        }catch (Exception ex){
            log.error("get exception", ex);
            return BaseResult.fail(ErrorCode.URL_GET_EXCEPTION.getCode(), ex.getMessage());
        }
    }

}
