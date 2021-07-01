package com.vipgp.tinyurl.dubbo.provider.service.checker;

import com.vipgp.tinyurl.dubbo.provider.dto.CreateDTO;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 基于spring注解实现职责链模式
 * @author: linshangdou@gmail.com
 * @date: 2021/4/19 13:49
 */
@Component
@Validated
public class CreationCheckers extends CheckerRegister<CreateDTO> {
}
