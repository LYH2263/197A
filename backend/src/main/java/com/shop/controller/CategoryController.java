package com.shop.controller;

import com.shop.common.Result;
import com.shop.entity.Category;
import com.shop.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类接口
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public Result<List<Category>> listAll() {
        return Result.ok(categoryService.listAll());
    }

    @GetMapping("/children")
    public Result<List<Category>> listByParent(@RequestParam(required = false) Long parentId) {
        return Result.ok(categoryService.listByParentId(parentId));
    }
}
