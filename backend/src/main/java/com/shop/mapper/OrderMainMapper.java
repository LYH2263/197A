package com.shop.mapper;

import com.shop.entity.OrderMain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单主表 Mapper
 */
@Mapper
public interface OrderMainMapper {

    int insert(OrderMain orderMain);

    OrderMain selectById(@Param("id") Long id);

    OrderMain selectByOrderNo(@Param("orderNo") String orderNo);

    List<OrderMain> selectByUserId(@Param("userId") Long userId);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
