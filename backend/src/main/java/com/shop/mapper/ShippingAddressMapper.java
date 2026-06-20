package com.shop.mapper;

import com.shop.entity.ShippingAddress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShippingAddressMapper {

    int insert(ShippingAddress address);

    int update(ShippingAddress address);

    int deleteById(@Param("id") Long id, @Param("userId") Long userId);

    ShippingAddress selectById(@Param("id") Long id);

    List<ShippingAddress> selectByUserId(@Param("userId") Long userId);

    ShippingAddress selectDefaultByUserId(@Param("userId") Long userId);

    int clearDefaultByUserId(@Param("userId") Long userId);

    int updateDisabled(@Param("id") Long id, @Param("isDisabled") Integer isDisabled);

    int updateIsDefault(@Param("id") Long id, @Param("isDefault") Integer isDefault);
}
