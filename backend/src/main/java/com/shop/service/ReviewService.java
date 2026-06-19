package com.shop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.dto.ReviewReplyRequest;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品评价服务
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);
    private static final int FOLLOWUP_DAYS = 7;
    private static final int MAX_IMAGES = 3;
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB

    private final ReviewMapper reviewMapper;
    private final OrderMainMapper orderMainMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public Review create(Long userId, ReviewRequest req) {
        if (req.getParentId() != null) {
            return createFollowup(userId, req);
        }
        return createInitial(userId, req);
    }

    @Transactional(rollbackFor = Exception.class)
    public Review createInitial(Long userId, ReviewRequest req) {
        validateImages(req.getImages());

        OrderMain order = orderMainMapper.selectById(req.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (order.getStatus() == null || order.getStatus() < 1 || order.getStatus() > 3) {
            throw new IllegalArgumentException("仅已付款及之后的订单可评价");
        }
        if (reviewMapper.selectInitialByUserAndOrderAndProduct(userId, req.getOrderId(), req.getProductId()) != null) {
            throw new IllegalArgumentException("该订单商品已评价");
        }

        Review review = new Review();
        review.setParentId(null);
        review.setReviewType(Review.TYPE_INITIAL);
        review.setUserId(userId);
        review.setProductId(req.getProductId());
        review.setOrderId(req.getOrderId());
        review.setRating(req.getRating());
        review.setContent(req.getContent());
        review.setImages(toJson(req.getImages()));
        reviewMapper.insert(review);

        log.info("Initial review created: productId={}, userId={}, reviewId={}", req.getProductId(), userId, review.getId());
        return review;
    }

    @Transactional(rollbackFor = Exception.class)
    public Review createFollowup(Long userId, ReviewRequest req) {
        Review parent = reviewMapper.selectById(req.getParentId());
        if (parent == null) {
            throw new IllegalArgumentException("父评价不存在");
        }
        if (parent.getReviewType() != Review.TYPE_INITIAL) {
            throw new IllegalArgumentException("仅能对初评进行追评");
        }
        if (!parent.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只能追评自己的评价");
        }

        long daysSinceCreated = Duration.between(parent.getCreatedAt(), LocalDateTime.now()).toDays();
        if (daysSinceCreated > FOLLOWUP_DAYS) {
            throw new IllegalArgumentException("评价已超过" + FOLLOWUP_DAYS + "天，无法追评");
        }

        List<Review> existingFollowups = reviewMapper.selectByParentId(req.getParentId());
        if (!existingFollowups.isEmpty()) {
            throw new IllegalArgumentException("每条评价仅可追评一次");
        }

        OrderMain order = orderMainMapper.selectById(req.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("订单不存在");
        }

        Review followup = new Review();
        followup.setParentId(req.getParentId());
        followup.setReviewType(Review.TYPE_FOLLOWUP);
        followup.setUserId(userId);
        followup.setProductId(req.getProductId());
        followup.setOrderId(req.getOrderId());
        followup.setRating(req.getRating());
        followup.setContent(req.getContent());
        followup.setImages(null);
        reviewMapper.insert(followup);

        log.info("Followup review created: parentId={}, userId={}, followupId={}", req.getParentId(), userId, followup.getId());
        return followup;
    }

    @Transactional(rollbackFor = Exception.class)
    public Review update(Long userId, Long id, ReviewRequest req) {
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            throw new IllegalArgumentException("评价不存在");
        }
        if (!review.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只能编辑自己的评价");
        }
        if (review.getReviewType() != Review.TYPE_INITIAL) {
            throw new IllegalArgumentException("仅初评可编辑");
        }
        if (review.getReplyContent() != null) {
            throw new IllegalArgumentException("商家已回复，不可编辑");
        }

        validateImages(req.getImages());

        review.setContent(req.getContent());
        review.setImages(toJson(req.getImages()));
        reviewMapper.updateContent(review.getId(), review.getContent(), review.getImages());

        log.info("Review updated: id={}, userId={}", id, userId);
        return review;
    }

    @Transactional(rollbackFor = Exception.class)
    public void reply(Long adminId, String adminName, ReviewReplyRequest req) {
        Review review = reviewMapper.selectById(req.getReviewId());
        if (review == null) {
            throw new IllegalArgumentException("评价不存在");
        }
        if (review.getReviewType() != Review.TYPE_INITIAL) {
            throw new IllegalArgumentException("仅能对初评进行回复");
        }
        if (review.getReplyContent() != null) {
            throw new IllegalArgumentException("该评价已回复，不可重复回复");
        }

        reviewMapper.updateReply(review.getId(), req.getContent(), LocalDateTime.now());
        log.info("Review replied: reviewId={}, adminId={}, adminName={}", req.getReviewId(), adminId, adminName);
    }

    public List<ReviewVO> listByProductIdWithTimeline(Long productId, Long currentUserId) {
        List<Review> allReviews = reviewMapper.selectByProductId(productId);
        return buildTimeline(allReviews, currentUserId);
    }

    public List<ReviewVO> listByUserId(Long userId) {
        List<Review> list = reviewMapper.selectByUserId(userId);
        return buildTimeline(list, userId);
    }

    public List<ReviewVO> listAll() {
        List<Review> list = reviewMapper.selectAll();
        return buildTimeline(list, null);
    }

    private List<ReviewVO> buildTimeline(List<Review> reviews, Long currentUserId) {
        Map<Long, List<Review>> followupMap = reviews.stream()
                .filter(r -> r.getParentId() != null)
                .collect(Collectors.groupingBy(Review::getParentId));

        List<ReviewVO> result = new ArrayList<>();
        for (Review r : reviews) {
            if (r.getReviewType() == Review.TYPE_INITIAL) {
                ReviewVO vo = toReviewVO(r, currentUserId);
                List<Review> followups = followupMap.getOrDefault(r.getId(), Collections.emptyList());
                vo.setFollowups(followups.stream()
                        .map(f -> toReviewVO(f, currentUserId))
                        .collect(Collectors.toList()));
                result.add(vo);
            }
        }

        result.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return result;
    }

    private ReviewVO toReviewVO(Review r, Long currentUserId) {
        ReviewVO vo = new ReviewVO();
        vo.setId(r.getId());
        vo.setParentId(r.getParentId());
        vo.setReviewType(r.getReviewType());
        vo.setUserId(r.getUserId());
        vo.setProductId(r.getProductId());
        vo.setOrderId(r.getOrderId());
        vo.setRating(r.getRating());
        vo.setContent(r.getContent());
        vo.setImages(fromJson(r.getImages()));
        vo.setReplyContent(r.getReplyContent());
        vo.setReplyAt(r.getReplyAt());
        vo.setCreatedAt(r.getCreatedAt());

        Product p = productMapper.selectById(r.getProductId());
        vo.setProductName(p != null ? p.getName() : null);
        User u = userMapper.selectById(r.getUserId());
        vo.setUserName(u != null ? u.getUsername() : null);

        if (r.getReviewType() == Review.TYPE_INITIAL && currentUserId != null && r.getUserId().equals(currentUserId)) {
            long daysSinceCreated = Duration.between(r.getCreatedAt(), LocalDateTime.now()).toDays();
            List<Review> existingFollowups = reviewMapper.selectByParentId(r.getId());

            if (existingFollowups.isEmpty() && daysSinceCreated <= FOLLOWUP_DAYS && r.getReplyContent() == null) {
                vo.setCanFollowup(true);
                vo.setFollowupDisabledReason(null);
            } else if (!existingFollowups.isEmpty()) {
                vo.setCanFollowup(false);
                vo.setFollowupDisabledReason("已追加过追评");
            } else if (daysSinceCreated > FOLLOWUP_DAYS) {
                vo.setCanFollowup(false);
                vo.setFollowupDisabledReason("已超过" + FOLLOWUP_DAYS + "天追评期限");
            } else if (r.getReplyContent() != null) {
                vo.setCanFollowup(false);
                vo.setFollowupDisabledReason("商家已回复");
            }
            vo.setCanEdit(r.getReplyContent() == null);
        } else {
            vo.setCanFollowup(false);
            vo.setCanEdit(false);
        }

        return vo;
    }

    private void validateImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        if (images.size() > MAX_IMAGES) {
            throw new IllegalArgumentException("最多上传" + MAX_IMAGES + "张图片");
        }
        for (String url : images) {
            if (url == null || url.trim().isEmpty()) {
                throw new IllegalArgumentException("图片URL不能为空");
            }
            String lowerUrl = url.toLowerCase();
            boolean validExtension = ALLOWED_IMAGE_EXTENSIONS.stream().anyMatch(lowerUrl::endsWith);
            if (!validExtension) {
                throw new IllegalArgumentException("仅支持 " + ALLOWED_IMAGE_EXTENSIONS + " 格式的图片");
            }
        }
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.warn("Failed to serialize images to JSON", e);
            return null;
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize images from JSON: {}", json, e);
            return Collections.emptyList();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        if (reviewMapper.selectById(id) == null) {
            throw new IllegalArgumentException("评价不存在");
        }
        reviewMapper.deleteById(id);
        log.info("Review deleted: id={}", id);
    }
}
