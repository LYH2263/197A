package com.shop.mapper;

import com.shop.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品评价 Mapper
 */
@Mapper
public interface ReviewMapper {

    int insert(Review review);

    List<Review> selectByProductId(@Param("productId") Long productId);

    List<Review> selectByUserId(@Param("userId") Long userId);

    List<Review> selectAll();

    Review selectById(@Param("id") Long id);

    int deleteById(@Param("id") Long id);

    Review selectByUserAndOrderAndProduct(@Param("userId") Long userId,
                                          @Param("orderId") Long orderId,
                                          @Param("productId") Long productId);
}
