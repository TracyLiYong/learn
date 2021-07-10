package com.billow.system.dao;

import com.billow.system.pojo.po.MytasklistPo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.CacheNamespace;
import com.billow.mybatis.cache.MybatisRedisCache;

/**
 * <p>
 * VIEW Mapper 接口
 * </p>
 *
 * @author billow
 * @since 2021-04-01
 */
@CacheNamespace(implementation = MybatisRedisCache.class)
public interface MytasklistDao extends BaseMapper<MytasklistPo> {

}
