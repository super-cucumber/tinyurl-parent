package com.vipgp.tinyurl.api.controller;

import com.vipgp.tinyurl.api.vo.ResultVO;
import com.vipgp.tinyurl.dubbo.service.TinyurlService;
import com.vipgp.tinyurl.dubbo.service.dto.BaseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by Shangdu Lin on 2021/1/7 18:55.
 */
@RestController
@Slf4j
public class TinyurlController {

    @Reference(version = "1.0.0")
    TinyurlService tinyurlService;

    @Value("${tiny.url.404.page}")
    String notFoundPageUrl;

    @Autowired
    TinyurlServiceProviderCacheAop provider;


    /**
     * create tiny url base on longUrl
     * @param longUrl
     * @return
     */
    @PostMapping("/tiny/url/create")
    public ResultVO<String> createTinyurl(String longUrl, String baseUrl) {
        if (StringUtils.isEmpty(longUrl)) {
            return ResultVO.fail("urlempty", "url can't be empty");
        }
        long start=System.nanoTime();
        BaseResult<String> result = tinyurlService.createTinyurl(baseUrl.trim(),longUrl.trim());
        long end=System.nanoTime();
        log.info("call create tiny url dubbo service with no alias code, cost {}ns {}ms",end-start,(end-start)/1000000);
        if (result.isSuccess()) {
            return ResultVO.success(result.getResult());
        } else {
            return ResultVO.fail(result.getErrorCode(), result.getErrorMessage());
        }
    }

    /**
     * create tiny url base on longUrl and alias code which user inputted
     * @param diyCode
     * @param longUrl
     * @return
     */
    @PostMapping("/tiny/url/diy/create")
    public ResultVO<String> createDiyTinyurl(String diyCode, String longUrl, String baseUrl) {
        if (StringUtils.isEmpty(longUrl)) {
            return ResultVO.fail("urlempty", "url can't be empty");
        }
        if (StringUtils.isEmpty(diyCode)) {
            return ResultVO.fail("codeempty", "code can't be empty");
        }
        if (diyCode.length()>5) {
            return ResultVO.fail("codeinvalid", "code length can't be more than 5");
        }
        BaseResult<String> result=tinyurlService.createTinyurl(baseUrl.trim(),diyCode.trim(), longUrl.trim());
        if(result.isSuccess()) {
            return ResultVO.success(result.getResult());
        }else {
            return ResultVO.fail(result.getErrorCode(),result.getErrorMessage());
        }
    }

    /**
     * redirect to long url with http code 302
     * @param code
     * @return
     */
    @GetMapping("/{code}")
    public ModelAndView transferToLongUrl(@PathVariable String code, HttpServletRequest request) {
        if (StringUtils.isEmpty(code) || code.length()>7) {
            return new ModelAndView(new RedirectView(notFoundPageUrl));
        }
        log.info("serverName="+request.getServerName());
        BaseResult<String> result = provider.getLongUrl(request.getServerName(),code);
        log.info("result="+result);
        if (result.isSuccess()) {
            return new ModelAndView(new RedirectView(result.getResult()));
        } else {
            return new ModelAndView(new RedirectView(notFoundPageUrl));
        }
    }




}
