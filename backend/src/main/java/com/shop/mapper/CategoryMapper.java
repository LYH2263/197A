package com.shop.mapper;

import com.shop.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品分类 Mapper
 */
@Mapper
public interface CategoryMapper {

    List<Category> selectAll();

    List<Category> selectByParentId(@Param("parentId") Long parentId);

    Category selectById(@Param("id") Long id);
}
