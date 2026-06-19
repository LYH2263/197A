package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.ProductQueryDTO;
import com.shop.entity.Product;
import com.shop.entity.ProductImage;
import com.shop.service.ProductImageService;
import com.shop.service.ProductService;
import com.shop.service.BrowseHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductImageService productImageService;
    private final BrowseHistoryService browseHistoryService;

    @GetMapping
    public Result<List<Product>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        ProductQueryDTO query = new ProductQueryDTO();
        query.setCategoryId(categoryId);
        query.setKeyword(keyword);
        query.setMinPrice(minPrice);
        query.setMaxPrice(maxPrice);
        query.setStockStatus(stockStatus);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setSortBy(sortBy);
        query.setSortOrder(sortOrder);
        return Result.ok(productService.advancedList(query));
    }

    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id, Authentication auth) {
        Product p = productService.getById(id);
        if (p == null) {
            return Result.fail(404, "商品不存在");
        }
        Long userId = null;
        if (auth != null && auth.getPrincipal() instanceof Long uid) {
            userId = uid;
        }
        browseHistoryService.recordView(userId, p.getId(), p.getCategoryId());
        return Result.ok(p);
    }

    @GetMapping("/{id}/images")
    public Result<List<ProductImage>> getImages(@PathVariable Long id) {
        return Result.ok(productImageService.listByProductId(id));
    }
}
