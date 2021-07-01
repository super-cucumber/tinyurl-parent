package com.vipgp.tinyurl.dubbo.provider.aop;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/17 13:24
 */
@Slf4j
@Component
@Aspect
public class LogAspect extends LogBase {

    @Pointcut("execution(public * com.vipgp.tinyurl.dubbo.provider.service.impl..*.*(..))")
    public void dubboMethod(){}

    @Around("dubboMethod()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String requestString = toLog(joinPoint);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Object ret = null;
        try {
            ret = joinPoint.proceed();
        } finally {
            stopWatch.stop();
            log.info("request={},response={},cost={}ms", requestString, JSON.toJSONString(ret), stopWatch.getTotalTimeMillis());
        }

        return ret;
    }
}
