package com.vipgp.tinyurl.dubbo.provider.service.checker;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.util.List;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/4/18 16:07
 */
@Validated
public abstract class CheckerRegister<T> {

    @Autowired
    @Setter
    private List<Checker<T>> checkers;

    public void check(@Valid T entity){
        checkers.stream().forEach(checker->checker.check(entity));
    }
}
