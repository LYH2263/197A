package com.shop.mapper;

import com.shop.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 购物车项 Mapper
 */
@Mapper
public interface CartItemMapper {

    int insert(CartItem cartItem);

    int updateById(CartItem cartItem);

    CartItem selectByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    List<CartItem> selectByUserId(@Param("userId") Long userId);

    int deleteById(@Param("id") Long id);

    int deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    int updateChecked(@Param("userId") Long userId, @Param("productId") Long productId, @Param("checked") Integer checked);

    List<CartItem> selectCheckedByUserId(@Param("userId") Long userId);
}
