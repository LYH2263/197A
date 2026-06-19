package com.shop.mapper;

import com.shop.dto.AdminProductQueryDTO;
import com.shop.dto.ProductQueryDTO;
import com.shop.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品 Mapper
 */
@Mapper
public interface ProductMapper {

    List<Product> selectByCondition(@Param("categoryId") Long categoryId,
                                    @Param("keyword") String keyword,
                                    @Param("status") Integer status);

    List<Product> selectByAdvancedCondition(@Param("query") ProductQueryDTO query);

    Product selectById(@Param("id") Long id);

    int updateStock(@Param("id") Long id, @Param("quantity") int quantity);

    int updateMainImage(@Param("id") Long id, @Param("mainImage") String mainImage);

    int updatePrice(@Param("id") Long id, @Param("price") java.math.BigDecimal price);

    List<Product> selectByAdminCondition(@Param("query") AdminProductQueryDTO query);

    long countByAdminCondition(@Param("query") AdminProductQueryDTO query);

    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") Integer status);

    int batchUpdateCategory(@Param("ids") List<Long> ids, @Param("categoryId") Long categoryId);

    int batchUpdatePrice(@Param("id") Long id, @Param("price") BigDecimal price);

    int insertProduct(@Param("product") Product product);

    List<Product> selectByAdminConditionForExport(@Param("query") AdminProductQueryDTO query);

    List<Product> selectByIds(@Param("ids") List<Long> ids);
}
