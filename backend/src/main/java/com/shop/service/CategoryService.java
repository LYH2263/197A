package com.shop.service;

import com.shop.entity.Category;
import com.shop.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品分类服务
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public List<Category> listAll() {
        return categoryMapper.selectAll();
    }

    public List<Category> listByParentId(Long parentId) {
        return categoryMapper.selectByParentId(parentId == null ? 0L : parentId);
    }
}
