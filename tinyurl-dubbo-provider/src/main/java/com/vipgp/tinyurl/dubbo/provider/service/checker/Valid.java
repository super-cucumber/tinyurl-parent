package com.vipgp.tinyurl.dubbo.provider.service.checker;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/18 16:40
 */
@Target({ FIELD, TYPE})
@Retention(RUNTIME)
@Documented
public @interface Valid {
    Class<? extends Checker>[] value();
}
