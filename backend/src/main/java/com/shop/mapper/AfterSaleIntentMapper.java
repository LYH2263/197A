package com.shop.mapper;

import com.shop.entity.AfterSaleIntent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AfterSaleIntentMapper {

    int insert(AfterSaleIntent intent);
}
