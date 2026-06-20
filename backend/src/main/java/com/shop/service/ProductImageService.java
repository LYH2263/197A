package com.shop.service;

import com.shop.entity.Product;
import com.shop.entity.ProductImage;
import com.shop.mapper.ProductImageMapper;
import com.shop.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private static final int MIN_IMAGES = 3;
    private static final int MAX_IMAGES = 8;

    private final ProductImageMapper productImageMapper;
    private final ProductMapper productMapper;

    public List<ProductImage> listByProductId(Long productId) {
        List<ProductImage> raw = productImageMapper.selectByProductId(productId);
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        Map<String, ProductImage> dedup = new LinkedHashMap<>();
        for (ProductImage img : raw) {
            String key = img.getImageUrl();
            if (!dedup.containsKey(key)) {
                dedup.put(key, img);
            }
        }
        List<ProductImage> result = new ArrayList<>(dedup.values());
        result.sort(Comparator.comparingInt(ProductImage::getSortOrder)
                .thenComparing(ProductImage::getId));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public ProductImage addImage(Long productId, String imageUrl, Integer sortOrder) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("图片URL不能为空");
        }
        imageUrl = imageUrl.trim();
        int count = productImageMapper.countByProductId(productId);
        if (count >= MAX_IMAGES) {
            throw new IllegalArgumentException("每个商品最多" + MAX_IMAGES + "张展示图，当前已达上限");
        }
        if (productImageMapper.existsByProductIdAndImageUrl(productId, imageUrl) > 0) {
            throw new IllegalArgumentException("该图片URL已存在，请更换不同的图片");
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
        if (urls == null || urls.isEmpty()) {
            throw new IllegalArgumentException("URL列表不能为空");
        }
        int currentCount = productImageMapper.countByProductId(productId);
        List<String> cleanedUrls = urls.stream()
                .map(String::trim)
                .filter(u -> !u.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        List<String> duplicates = new ArrayList<>();
        List<String> newUrls = new ArrayList<>();
        for (String url : cleanedUrls) {
            if (productImageMapper.existsByProductIdAndImageUrl(productId, url) > 0) {
                duplicates.add(url);
            } else {
                newUrls.add(url);
            }
        }
        if (newUrls.isEmpty()) {
            if (!duplicates.isEmpty()) {
                throw new IllegalArgumentException("所有URL均已存在，未新增任何图片（重复数量：" + duplicates.size() + "）");
            }
            throw new IllegalArgumentException("没有有效的URL可导入");
        }
        if (currentCount + newUrls.size() > MAX_IMAGES) {
            throw new IllegalArgumentException("超过" + MAX_IMAGES + "张上限，当前已有" + currentCount + "张，本次去重后可导入" + newUrls.size() + "张，最多还能添加" + (MAX_IMAGES - currentCount) + "张");
        }
        List<ProductImage> images = new ArrayList<>();
        for (int i = 0; i < newUrls.size(); i++) {
            ProductImage img = new ProductImage();
            img.setProductId(productId);
            img.setImageUrl(newUrls.get(i));
            img.setSortOrder(currentCount + i);
            img.setIsMain(currentCount == 0 && i == 0 ? 1 : 0);
            images.add(img);
        }
        productImageMapper.insertBatch(images);
        if (currentCount == 0 && !images.isEmpty()) {
            syncMainImageToProduct(productId, images.get(0).getImageUrl());
        }
        return images;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteImage(Long id) {
        ProductImage img = productImageMapper.selectById(id);
        if (img == null) {
            throw new IllegalArgumentException("图片不存在");
        }
        int count = productImageMapper.countByProductId(img.getProductId());
        if (count <= MIN_IMAGES) {
            throw new IllegalArgumentException("每个商品至少保留" + MIN_IMAGES + "张展示图，当前共" + count + "张，无法继续删除");
        }
        productImageMapper.deleteById(id);
        if (img.getIsMain() == 1) {
            List<ProductImage> remaining = listByProductId(img.getProductId());
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
        List<ProductImage> existing = listByProductId(productId);
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
