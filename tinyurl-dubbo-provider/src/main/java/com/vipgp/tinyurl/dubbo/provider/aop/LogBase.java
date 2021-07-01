package com.vipgp.tinyurl.dubbo.provider.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/17 13:34
 */
@Slf4j
public class LogBase {

    /**
     * 打印输入
     * @param joinPoint
     * @return
     */
    protected String toLog(ProceedingJoinPoint joinPoint) {
        try {
            //获取类的字节码对象，通过字节码对象获取方法信息
            Class<?> targetCls = joinPoint.getTarget().getClass();
            //获取方法签名(通过此签名获取目标方法信息)
            MethodSignature ms = (MethodSignature) joinPoint.getSignature();
            //获取目标方法上的注解指定的操作名称
            Method targetMethod = targetCls.getDeclaredMethod(ms.getName(), ms.getParameterTypes());
            //获取请求参数
            String targetMethodParams = Arrays.toString(joinPoint.getArgs());

            return targetMethod + "-" + targetMethodParams;
        } catch (Exception ex) {
            log.error("joint point exception", ex);
        }

        return null;
    }
}
