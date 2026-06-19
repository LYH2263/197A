package com.shop.mapper;

import com.shop.entity.SaveForLater;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SaveForLaterMapper {

    int insert(SaveForLater item);

    int updateById(SaveForLater item);

    SaveForLater selectByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    List<SaveForLater> selectByUserId(@Param("userId") Long userId);

    int deleteById(@Param("id") Long id);

    int deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    int deleteByUserIdAndProductIds(@Param("userId") Long userId, @Param("productIds") List<Long> productIds);

    int countByUserId(@Param("userId") Long userId);
}
