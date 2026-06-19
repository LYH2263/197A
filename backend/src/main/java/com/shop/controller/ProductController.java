package com.shop.controller;

import com.shop.common.Result;
import com.shop.entity.Product;
import com.shop.entity.ProductImage;
import com.shop.service.ProductImageService;
import com.shop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductImageService productImageService;

    @GetMapping
    public Result<List<Product>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return Result.ok(productService.list(categoryId, keyword));
    }

    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id) {
        Product p = productService.getById(id);
        if (p == null) {
            return Result.fail(404, "商品不存在");
        }
        return Result.ok(p);
    }

    @GetMapping("/{id}/images")
    public Result<List<ProductImage>> getImages(@PathVariable Long id) {
        return Result.ok(productImageService.listByProductId(id));
    }
}
