package com.shop.mapper;

import com.shop.entity.OrderShareToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderShareTokenMapper {

    int insert(OrderShareToken token);

    OrderShareToken selectByToken(@Param("token") String token);
}
