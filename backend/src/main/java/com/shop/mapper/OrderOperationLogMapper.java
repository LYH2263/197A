package com.shop.mapper;

import com.shop.entity.OrderOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderOperationLogMapper {

    int insert(OrderOperationLog log);
}
