package com.vipgp.tinyurl.dubbo.provider.service.impl;

import com.vipgp.tinyurl.dubbo.provider.tasks.NamedThreadFactory;
import com.vipgp.tinyurl.dubbo.provider.util.ErrorCode;
import com.vipgp.tinyurl.dubbo.service.TinyurlService;
import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Service;
import org.apache.skywalking.apm.toolkit.trace.SupplierWrapper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * author: Sando
 * date: 2021/1/10
 */
@Slf4j
@Service(version = "2.0.0",interfaceClass = TinyurlService.class)
public class TinyurlServiceImplV2 extends TinyurlServiceImpl implements TinyurlService {

    int processorCount=Runtime.getRuntime().availableProcessors();
    private ThreadPoolExecutor executor=new ThreadPoolExecutor(processorCount,processorCount,0L, TimeUnit.SECONDS,
            new LinkedBlockingDeque<>(),new NamedThreadFactory("TinyUrl-Add"));

    private BaseResult<String> parentCreateTinyurl(String baseUrl, String rawUrl) {
        return super.createTinyurl(baseUrl, rawUrl);
    }

    private BaseResult<String> parentCreateTinyurl(String baseUrl,String aliasCode, String rawUrl){
        return super.createTinyurl(baseUrl, aliasCode, rawUrl);
    }

    /**
     * generate alias code by system
     * @param baseUrl
     * @param rawUrl
     * @return
     */
    @Override
    public BaseResult<String> createTinyurl(String baseUrl, String rawUrl) {
        log.info("completable future no alias code run begin");
        CompletableFuture<BaseResult<String>> future=CompletableFuture.supplyAsync(SupplierWrapper.of(new Supplier<BaseResult<String>>() {
            @Override
            public BaseResult<String> get() {
                return parentCreateTinyurl(baseUrl, rawUrl);
            }
        }),executor);

        try {
            BaseResult<String> result= future.get();
            log.info("completable future no alias code run end");
            return result;
        }catch (Exception ex){
            return BaseResult.fail(ErrorCode.FUTURE_GET_FAIL.getCode(),ErrorCode.FUTURE_GET_FAIL.getInfo());
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
        log.info("completable future alias code run begin");
        CompletableFuture<BaseResult<String>> future=CompletableFuture.supplyAsync(SupplierWrapper.of(new Supplier<BaseResult<String>>() {
            @Override
            public BaseResult<String> get() {
                return parentCreateTinyurl(baseUrl, aliasCode, rawUrl);
            }
        }),executor);

        try {
            BaseResult<String> result= future.get();
            log.info("completable future alias code run end");
            return result;
        }catch (Exception ex){
            return BaseResult.fail(ErrorCode.FUTURE_GET_FAIL.getCode(),ErrorCode.FUTURE_GET_FAIL.getInfo());
        }
    }


}
