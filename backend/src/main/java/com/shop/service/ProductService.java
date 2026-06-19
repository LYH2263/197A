package com.shop.service;

import com.shop.entity.Product;
import com.shop.entity.ProductImage;
import com.shop.mapper.ProductImageMapper;
import com.shop.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;

    public List<Product> list(Long categoryId, String keyword) {
        List<Product> products = productMapper.selectByCondition(categoryId, keyword, 1);
        for (Product p : products) {
            List<ProductImage> images = productImageMapper.selectByProductId(p.getId());
            if (images != null && !images.isEmpty()) {
                p.setImages(images);
            }
        }
        return products;
    }

    public Product getById(Long id) {
        Product p = productMapper.selectById(id);
        if (p != null) {
            List<ProductImage> images = productImageMapper.selectByProductId(id);
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
}
