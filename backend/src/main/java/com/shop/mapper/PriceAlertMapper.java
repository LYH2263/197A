package com.shop.mapper;

import com.shop.entity.PriceAlert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface PriceAlertMapper {

    int insert(PriceAlert alert);

    PriceAlert selectByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    int updateNotified(@Param("id") Long id, @Param("notified") Integer notified);

    int deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    List<PriceAlert> selectByUserId(@Param("userId") Long userId);

    List<PriceAlert> selectUnnotifiedByProductId(@Param("productId") Long productId, @Param("currentPrice") BigDecimal currentPrice);
}
