package com.shop.service;

import com.shop.entity.Product;
import com.shop.entity.ProductImage;
import com.shop.mapper.ProductImageMapper;
import com.shop.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductImageMapper productImageMapper;
    private final ProductMapper productMapper;

    public List<ProductImage> listByProductId(Long productId) {
        return productImageMapper.selectByProductId(productId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProductImage addImage(Long productId, String imageUrl, Integer sortOrder) {
        int count = productImageMapper.countByProductId(productId);
        if (count >= 8) {
            throw new IllegalArgumentException("每个商品最多8张展示图");
        }
        if (sortOrder == null) {
            sortOrder = count;
        }
        ProductImage img = new ProductImage();
        img.setProductId(productId);
        img.setImageUrl(imageUrl);
        img.setSortOrder(sortOrder);
        img.setIsMain(count == 0 ? 1 : 0);
        productImageMapper.insert(img);
        if (count == 0) {
            syncMainImageToProduct(productId, imageUrl);
        }
        return img;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<ProductImage> bulkImport(Long productId, List<String> urls) {
        int currentCount = productImageMapper.countByProductId(productId);
        if (currentCount + urls.size() > 8) {
            throw new IllegalArgumentException("超过8张上限，当前已有" + currentCount + "张，最多还能添加" + (8 - currentCount) + "张");
        }
        List<ProductImage> images = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i).trim();
            if (url.isEmpty()) continue;
            ProductImage img = new ProductImage();
            img.setProductId(productId);
            img.setImageUrl(url);
            img.setSortOrder(currentCount + i);
            img.setIsMain(currentCount == 0 && i == 0 ? 1 : 0);
            images.add(img);
        }
        if (!images.isEmpty()) {
            productImageMapper.insertBatch(images);
            if (currentCount == 0) {
                syncMainImageToProduct(productId, images.get(0).getImageUrl());
            }
        }
        return images;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteImage(Long id) {
        ProductImage img = productImageMapper.selectById(id);
        if (img == null) {
            throw new IllegalArgumentException("图片不存在");
        }
        productImageMapper.deleteById(id);
        if (img.getIsMain() == 1) {
            List<ProductImage> remaining = productImageMapper.selectByProductId(img.getProductId());
            if (!remaining.isEmpty()) {
                productImageMapper.clearMainByProductId(img.getProductId());
                productImageMapper.setMain(remaining.get(0).getId());
                syncMainImageToProduct(img.getProductId(), remaining.get(0).getImageUrl());
            } else {
                syncMainImageToProduct(img.getProductId(), null);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void setMainImage(Long id) {
        ProductImage img = productImageMapper.selectById(id);
        if (img == null) {
            throw new IllegalArgumentException("图片不存在");
        }
        productImageMapper.clearMainByProductId(img.getProductId());
        productImageMapper.setMain(id);
        syncMainImageToProduct(img.getProductId(), img.getImageUrl());
    }

    @Transactional(rollbackFor = Exception.class)
    public void reorder(Long productId, List<Long> orderedIds) {
        List<ProductImage> existing = productImageMapper.selectByProductId(productId);
        Map<Long, ProductImage> map = existing.stream()
                .collect(Collectors.toMap(ProductImage::getId, Function.identity()));
        for (int i = 0; i < orderedIds.size(); i++) {
            Long imgId = orderedIds.get(i);
            ProductImage img = map.get(imgId);
            if (img != null) {
                productImageMapper.updateSortOrder(imgId, i);
            }
        }
    }

    private void syncMainImageToProduct(Long productId, String imageUrl) {
        Product product = productMapper.selectById(productId);
        if (product != null) {
            product.setMainImage(imageUrl);
            productMapper.updateMainImage(productId, imageUrl);
        }
    }
}
