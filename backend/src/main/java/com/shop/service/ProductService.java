package com.shop.service;

import com.shop.entity.Product;
import com.shop.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品服务
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;

    public List<Product> list(Long categoryId, String keyword) {
        return productMapper.selectByCondition(categoryId, keyword, 1);
    }

    public Product getById(Long id) {
        return productMapper.selectById(id);
    }

    public void decreaseStock(Long productId, int quantity) {
        int rows = productMapper.updateStock(productId, quantity);
        if (rows == 0) {
            throw new IllegalStateException("库存不足");
        }
    }
}
