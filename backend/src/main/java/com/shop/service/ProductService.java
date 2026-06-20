package com.shop.service;

import com.shop.dto.AdminProductQueryDTO;
import com.shop.dto.PageResult;
import com.shop.dto.ProductQueryDTO;
import com.shop.entity.Product;
import com.shop.entity.ProductImage;
import com.shop.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductImageService productImageService;

    public List<Product> list(Long categoryId, String keyword) {
        List<Product> products = productMapper.selectByCondition(categoryId, keyword, 1);
        for (Product p : products) {
            List<ProductImage> images = productImageService.listByProductId(p.getId());
            if (images != null && !images.isEmpty()) {
                p.setImages(images);
            }
        }
        return products;
    }

    public List<Product> advancedList(ProductQueryDTO query) {
        if (query.getStatus() == null) {
            query.setStatus(1);
        }
        List<Product> products = productMapper.selectByAdvancedCondition(query);
        for (Product p : products) {
            List<ProductImage> images = productImageService.listByProductId(p.getId());
            if (images != null && !images.isEmpty()) {
                p.setImages(images);
            }
        }
        return products;
    }

    public Product getById(Long id) {
        Product p = productMapper.selectById(id);
        if (p != null) {
            List<ProductImage> images = productImageService.listByProductId(id);
            if (images != null && !images.isEmpty()) {
                p.setImages(images);
            }
        }
        return p;
    }

    public void decreaseStock(Long productId, int quantity) {
        int rows = productMapper.updateStock(productId, quantity);
        if (rows == 0) {
            throw new IllegalStateException("库存不足");
        }
    }

    public PageResult<Product> adminList(AdminProductQueryDTO query) {
        if (query.getPageNum() == null || query.getPageNum() < 1) {
            query.setPageNum(1);
        }
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            query.setPageSize(10);
        }
        List<Product> list = productMapper.selectByAdminCondition(query);
        long total = productMapper.countByAdminCondition(query);
        return new PageResult<>(list, total, query.getPageNum(), query.getPageSize());
    }

    public List<Product> getByIds(List<Long> ids) {
        return productMapper.selectByIds(ids);
    }

    public List<Product> adminListForExport(AdminProductQueryDTO query) {
        return productMapper.selectByAdminConditionForExport(query);
    }
}
