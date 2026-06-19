package com.shop.service;

import com.shop.dto.ReviewRequest;
import com.shop.dto.ReviewVO;
import com.shop.entity.Product;
import com.shop.entity.Review;
import com.shop.entity.User;
import com.shop.mapper.OrderMainMapper;
import com.shop.mapper.ProductMapper;
import com.shop.mapper.ReviewMapper;
import com.shop.mapper.UserMapper;
import com.shop.entity.OrderMain;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品评价服务
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewMapper reviewMapper;
    private final OrderMainMapper orderMainMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    @Transactional(rollbackFor = Exception.class)
    public Review create(Long userId, ReviewRequest req) {
        OrderMain order = orderMainMapper.selectById(req.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("订单不存在");
        }
        // 已付款(1)、已发货(2)、已完成(3) 均可评价
        if (order.getStatus() == null || order.getStatus() < 1 || order.getStatus() > 3) {
            throw new IllegalArgumentException("仅已付款及之后的订单可评价");
        }
        if (reviewMapper.selectByUserAndOrderAndProduct(userId, req.getOrderId(), req.getProductId()) != null) {
            throw new IllegalArgumentException("该订单商品已评价");
        }
        Review review = new Review();
        review.setUserId(userId);
        review.setProductId(req.getProductId());
        review.setOrderId(req.getOrderId());
        review.setRating(req.getRating());
        review.setContent(req.getContent());
        reviewMapper.insert(review);
        log.info("Review created: productId={}, userId={}", req.getProductId(), userId);
        return review;
    }

    public List<Review> listByProductId(Long productId) {
        return reviewMapper.selectByProductId(productId);
    }

    /** 当前用户的评价列表（我的评价） */
    public List<ReviewVO> listByUserId(Long userId) {
        List<Review> list = reviewMapper.selectByUserId(userId);
        return toReviewVOList(list);
    }

    /** 全部评价（管理员） */
    public List<ReviewVO> listAll() {
        List<Review> list = reviewMapper.selectAll();
        return toReviewVOList(list);
    }

    /** 删除评价（管理员） */
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        if (reviewMapper.selectById(id) == null) {
            throw new IllegalArgumentException("评价不存在");
        }
        reviewMapper.deleteById(id);
        log.info("Review deleted: id={}", id);
    }

    private List<ReviewVO> toReviewVOList(List<Review> list) {
        List<ReviewVO> result = new ArrayList<>();
        for (Review r : list) {
            ReviewVO vo = new ReviewVO();
            vo.setId(r.getId());
            vo.setUserId(r.getUserId());
            vo.setProductId(r.getProductId());
            vo.setOrderId(r.getOrderId());
            vo.setRating(r.getRating());
            vo.setContent(r.getContent());
            vo.setCreatedAt(r.getCreatedAt());
            Product p = productMapper.selectById(r.getProductId());
            vo.setProductName(p != null ? p.getName() : null);
            User u = userMapper.selectById(r.getUserId());
            vo.setUserName(u != null ? u.getUsername() : null);
            result.add(vo);
        }
        return result;
    }
}
