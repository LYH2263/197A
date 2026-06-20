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

    int updatePaid(@Param("id") Long id,
                   @Param("status") Integer status,
                   @Param("paidAt") java.time.LocalDateTime paidAt);

    int countPendingByAddressId(@Param("addressId") Long addressId, @Param("userId") Long userId);

    int updateShipInfo(@Param("id") Long id,
                       @Param("status") Integer status,
                       @Param("logisticsCompany") String logisticsCompany,
                       @Param("trackingNo") String trackingNo,
                       @Param("shippingRemark") String shippingRemark,
                       @Param("shippedAt") java.time.LocalDateTime shippedAt);

    int updateReceive(@Param("id") Long id,
                      @Param("status") Integer status,
                      @Param("completedAt") java.time.LocalDateTime completedAt);

    List<OrderMain> selectAll();

    List<OrderMain> selectByStatus(@Param("status") Integer status);
}
