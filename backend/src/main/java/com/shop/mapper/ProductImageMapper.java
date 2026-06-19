package com.shop.mapper;

import com.shop.entity.ProductImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductImageMapper {

    List<ProductImage> selectByProductId(@Param("productId") Long productId);

    ProductImage selectById(@Param("id") Long id);

    int insert(ProductImage productImage);

    int insertBatch(@Param("list") List<ProductImage> list);

    int deleteById(@Param("id") Long id);

    int deleteByProductId(@Param("productId") Long productId);

    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") Integer sortOrder);

    int clearMainByProductId(@Param("productId") Long productId);

    int setMain(@Param("id") Long id);

    int countByProductId(@Param("productId") Long productId);
}
