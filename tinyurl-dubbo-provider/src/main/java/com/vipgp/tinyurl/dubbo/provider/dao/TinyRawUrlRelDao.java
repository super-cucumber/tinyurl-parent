package com.vipgp.tinyurl.dubbo.provider.dao;


import com.vipgp.tinyurl.dubbo.provider.domain.TinyRawUrlRelDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * author: linshangdou@gmail.com
 * date: 2021/1/13
 */
@Mapper
public interface TinyRawUrlRelDao {

    /**
     * 新增插入
     *
     * @param entity
     */
    void add(TinyRawUrlRelDO entity);

    /**
     * 根据联合主键获取记录
     * @param query
     * @return
     */
    TinyRawUrlRelDO get(TinyRawUrlRelDO query);

    /**
     * 批量插入
     * @param list
     */
    void batchInsert(List<TinyRawUrlRelDO> list);
}
