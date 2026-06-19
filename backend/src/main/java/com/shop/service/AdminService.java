package com.shop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.OrderStatus;
import com.shop.dto.ReviewVO;
import com.shop.dto.ShipRequest;
import com.shop.dto.UserVO;
import com.shop.entity.OrderMain;
import com.shop.entity.OrderOperationLog;
import com.shop.entity.User;
import com.shop.mapper.OrderMainMapper;
import com.shop.mapper.OrderOperationLogMapper;
import com.shop.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final UserMapper userMapper;
    private final ReviewService reviewService;
    private final OrderMainMapper orderMainMapper;
    private final OrderOperationLogMapper orderOperationLogMapper;

    public List<UserVO> listUsers() {
        List<User> list = userMapper.selectAll();
        List<UserVO> result = new ArrayList<>();
        for (User u : list) {
            UserVO vo = new UserVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setNickname(u.getNickname());
            vo.setPhone(u.getPhone());
            vo.setEmail(u.getEmail());
            vo.setAvatar(u.getAvatar());
            vo.setStatus(u.getStatus());
            vo.setRole(u.getRole());
            vo.setCreatedAt(u.getCreatedAt());
            result.add(vo);
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long id, Integer status, String role) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (status != null) {
            user.setStatus(status);
        }
        if (role != null && !role.isEmpty()) {
            user.setRole(role);
        }
        userMapper.updateById(user);
        log.info("Admin updated user: id={}, status={}, role={}", id, status, role);
    }

    public List<ReviewVO> listAllReviews() {
        return reviewService.listAll();
    }

    public void deleteReview(Long id) {
        reviewService.deleteById(id);
    }

    public List<OrderMain> listAllOrders() {
        return orderMainMapper.selectAll();
    }

    public List<OrderMain> listOrdersByStatus(Integer status) {
        if (status == null) {
            return orderMainMapper.selectAll();
        }
        return orderMainMapper.selectByStatus(status);
    }

    public OrderMain getOrderById(Long id) {
        return orderMainMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void ship(ShipRequest req, Long adminId, String adminName) {
        if (req == null || req.getOrderId() == null) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        if (req.getLogisticsCompany() == null || req.getLogisticsCompany().trim().isEmpty()) {
            throw new IllegalArgumentException("物流公司不能为空");
        }
        if (req.getTrackingNo() == null || req.getTrackingNo().trim().isEmpty()) {
            throw new IllegalArgumentException("运单号不能为空");
        }
        OrderMain order = orderMainMapper.selectById(req.getOrderId());
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        int from = order.getStatus();
        int to = OrderStatus.SHIPPED;
        if (!OrderStatus.canTransition(from, to)) {
            throw new IllegalArgumentException("订单状态不允许发货：当前 " + OrderStatus.text(from)
                    + "，仅「已付款」可发货");
        }
        orderMainMapper.updateShipInfo(
                req.getOrderId(),
                to,
                req.getLogisticsCompany().trim(),
                req.getTrackingNo().trim(),
                req.getShippingRemark() != null ? req.getShippingRemark().trim() : null,
                LocalDateTime.now()
        );
        try {
            OrderOperationLog opl = new OrderOperationLog();
            opl.setOrderId(order.getId());
            opl.setOrderNo(order.getOrderNo());
            opl.setOperatorId(adminId);
            opl.setOperatorName(adminName != null ? adminName : ("admin-" + adminId));
            opl.setOperatorRole("admin");
            opl.setOperation(OrderStatus.OP_SHIP);
            opl.setOldStatus(from);
            opl.setNewStatus(to);
            opl.setRemark(req.getShippingRemark());
            Map<String, Object> extra = new HashMap<>();
            extra.put("logisticsCompany", req.getLogisticsCompany().trim());
            extra.put("trackingNo", req.getTrackingNo().trim());
            try {
                opl.setExtraInfo(OBJECT_MAPPER.writeValueAsString(extra));
            } catch (JsonProcessingException ignored) {}
            orderOperationLogMapper.insert(opl);
        } catch (Exception e) {
            log.warn("写入订单操作日志失败", e);
        }
        log.info("Admin shipped order: orderId={}, company={}, trackingNo={}",
                req.getOrderId(), req.getLogisticsCompany(), req.getTrackingNo());
    }
}
