package com.shop.mapper;

import com.shop.entity.SiteConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SiteConfigMapper {

    SiteConfig selectByKey(@Param("configKey") String configKey);

    int upsert(@Param("configKey") String configKey, @Param("configValue") String configValue);
}
