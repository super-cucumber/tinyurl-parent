package com.vipgp.tinyurl.dubbo.provider.dao;

import com.vipgp.tinyurl.dubbo.provider.domain.LookupDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author: linshangdou@gmail.com
 * @date: 2021/3/15 9:44
 */
@Mapper
public interface LookupDao {

    List<LookupDO> queryAll();
}
