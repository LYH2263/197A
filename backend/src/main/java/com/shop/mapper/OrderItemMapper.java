package com.shop.mapper;

import com.shop.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单明细 Mapper
 */
@Mapper
public interface OrderItemMapper {

    int insert(OrderItem item);

    List<OrderItem> selectByOrderId(@Param("orderId") Long orderId);
}
