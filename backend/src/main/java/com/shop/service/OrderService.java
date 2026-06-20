package com.shop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.OrderStatus;
import com.shop.dto.OrderCreateRequest;
import com.shop.entity.*;
import com.shop.mapper.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final ShippingAddressMapper shippingAddressMapper;
    private final ShippingAddressService shippingAddressService;
    private final AfterSaleIntentMapper afterSaleIntentMapper;
    private final OrderShareTokenMapper orderShareTokenMapper;
    private final OrderOperationLogMapper orderOperationLogMapper;

    private void writeLog(Long orderId, String orderNo, Long operatorId, String operatorName,
                          String operatorRole, String operation, Integer oldStatus, Integer newStatus,
                          String remark, Map<String, Object> extra) {
        try {
            OrderOperationLog opl = new OrderOperationLog();
            opl.setOrderId(orderId);
            opl.setOrderNo(orderNo);
            opl.setOperatorId(operatorId);
            opl.setOperatorName(operatorName);
            opl.setOperatorRole(operatorRole);
            opl.setOperation(operation);
            opl.setOldStatus(oldStatus);
            opl.setNewStatus(newStatus);
            opl.setRemark(remark);
            if (extra != null && !extra.isEmpty()) {
                try {
                    opl.setExtraInfo(OBJECT_MAPPER.writeValueAsString(extra));
                } catch (JsonProcessingException ignored) {}
            }
            orderOperationLogMapper.insert(opl);
        } catch (Exception e) {
            log.warn("写入订单操作日志失败", e);
        }
    }

    private static String resolveUserName(Long userId) {
        return "user-" + userId;
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderMain create(Long userId, OrderCreateRequest req) {
        String receiverName;
        String receiverPhone;
        String fullAddress;
        Long shippingAddressId = null;

        if (req.getShippingAddressId() != null) {
            ShippingAddress addr = shippingAddressMapper.selectById(req.getShippingAddressId());
            if (addr == null || !addr.getUserId().equals(userId)) {
                throw new IllegalArgumentException("所选地址不存在");
            }
            receiverName = addr.getReceiverName();
            receiverPhone = addr.getReceiverPhone();
            fullAddress = addr.getProvince() + addr.getCity() + addr.getDistrict() + addr.getDetailAddress();
            shippingAddressId = addr.getId();
        } else {
            receiverName = req.getReceiverName();
            receiverPhone = req.getReceiverPhone();
            if (req.getProvince() != null && req.getCity() != null && req.getDistrict() != null && req.getDetailAddress() != null) {
                fullAddress = req.getProvince() + req.getCity() + req.getDistrict() + req.getDetailAddress();
            } else {
                fullAddress = req.getReceiverAddress();
            }

            if (receiverName == null || receiverName.trim().isEmpty()) {
                throw new IllegalArgumentException("收货人不能为空");
            }
            if (receiverPhone == null || receiverPhone.trim().isEmpty()) {
                throw new IllegalArgumentException("手机号不能为空");
            }
            if (!PHONE_PATTERN.matcher(receiverPhone).matches()) {
                throw new IllegalArgumentException("手机号格式不正确");
            }
            if (req.getProvince() == null || req.getProvince().trim().isEmpty()
                    || req.getCity() == null || req.getCity().trim().isEmpty()
                    || req.getDistrict() == null || req.getDistrict().trim().isEmpty()) {
                throw new IllegalArgumentException("请完整选择省市区");
            }
            if (req.getDetailAddress() == null || req.getDetailAddress().trim().isEmpty()) {
                throw new IllegalArgumentException("详细地址不能为空");
            }

            if (Boolean.TRUE.equals(req.getSaveToAddressBook())) {
                ShippingAddress addr = new ShippingAddress();
                addr.setUserId(userId);
                addr.setReceiverName(receiverName);
                addr.setReceiverPhone(receiverPhone);
                addr.setProvince(req.getProvince());
                addr.setCity(req.getCity());
                addr.setDistrict(req.getDistrict());
                addr.setDetailAddress(req.getDetailAddress());
                addr.setTag(req.getTag());
                addr.setIsDisabled(0);
                List<ShippingAddress> existing = shippingAddressMapper.selectByUserId(userId);
                if (existing.isEmpty()) {
                    shippingAddressMapper.clearDefaultByUserId(userId);
                    addr.setIsDefault(1);
                } else {
                    addr.setIsDefault(0);
                }
                shippingAddressMapper.insert(addr);
                shippingAddressId = addr.getId();
            }
        }

        List<CartItem> checked = cartItemMapper.selectCheckedByUserId(userId);
        if (checked.isEmpty()) {
            throw new IllegalArgumentException("请先勾选要结算的商品");
        }
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem item : checked) {
            Product p = productMapper.selectById(item.getProductId());
            if (p == null || p.getStock() < item.getQuantity()) {
                throw new IllegalStateException("商品 " + (p != null ? p.getName() : item.getProductId()) + " 库存不足");
            }
            totalAmount = totalAmount.add(p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        String orderNo = "O" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + String.format("%04d", SEQ.incrementAndGet() % 10000);
        OrderMain order = new OrderMain();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setReceiverAddress(fullAddress);
        order.setShippingAddressId(shippingAddressId);
        orderMainMapper.insert(order);
        for (CartItem item : checked) {
            Product p = productMapper.selectById(item.getProductId());
            productMapper.updateStock(p.getId(), item.getQuantity());
            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setProductId(p.getId());
            oi.setProductName(p.getName());
            oi.setProductImage(p.getMainImage());
            oi.setPrice(p.getPrice());
            oi.setQuantity(item.getQuantity());
            oi.setTotalAmount(p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            orderItemMapper.insert(oi);
            cartItemMapper.deleteByUserIdAndProductId(userId, item.getProductId());
        }
        writeLog(order.getId(), orderNo, userId, resolveUserName(userId), "user",
                OrderStatus.OP_CREATE, null, OrderStatus.PENDING_PAYMENT, "创建订单", null);
        log.info("Order created: orderNo={}, userId={}", orderNo, userId);
        if (shippingAddressId != null) {
            shippingAddressService.markDisabledIfReferenced(shippingAddressId, userId);
        }
        return orderMainMapper.selectById(order.getId());
    }

    public List<OrderMain> listByUserId(Long userId) {
        return orderMainMapper.selectByUserId(userId);
    }

    public OrderMain getById(Long id, Long userId) {
        OrderMain order = orderMainMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            return null;
        }
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public void pay(Long orderId, Long userId) {
        OrderMain order = orderMainMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("订单不存在");
        }
        int from = order.getStatus();
        int to = OrderStatus.PAID;
        if (!OrderStatus.canTransition(from, to)) {
            throw new IllegalArgumentException("订单状态不允许支付：当前 " + OrderStatus.text(from));
        }
        orderMainMapper.updateStatus(orderId, to);
        writeLog(orderId, order.getOrderNo(), userId, resolveUserName(userId), "user",
                OrderStatus.OP_PAY, from, to, "支付订单", null);
        log.info("Order paid: orderId={}", orderId);
        if (order.getShippingAddressId() != null) {
            shippingAddressService.markDisabledIfReferenced(order.getShippingAddressId(), userId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, Long userId) {
        OrderMain order = orderMainMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("订单不存在");
        }
        int from = order.getStatus();
        int to = OrderStatus.CANCELLED;
        if (!OrderStatus.canTransition(from, to)) {
            throw new IllegalArgumentException("当前状态不允许取消：" + OrderStatus.text(from));
        }
        orderMainMapper.updateStatus(orderId, to);
        writeLog(orderId, order.getOrderNo(), userId, resolveUserName(userId), "user",
                OrderStatus.OP_CANCEL, from, to, "取消订单", null);
        log.info("Order cancelled: orderId={}", orderId);
        if (order.getShippingAddressId() != null) {
            shippingAddressService.markDisabledIfReferenced(order.getShippingAddressId(), userId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void receive(Long orderId, Long userId) {
        OrderMain order = orderMainMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("订单不存在");
        }
        int from = order.getStatus();
        int to = OrderStatus.COMPLETED;
        if (!OrderStatus.canTransition(from, to)) {
            throw new IllegalArgumentException("当前状态不允许确认收货：" + OrderStatus.text(from));
        }
        orderMainMapper.updateReceive(orderId, to, LocalDateTime.now());
        writeLog(orderId, order.getOrderNo(), userId, resolveUserName(userId), "user",
                OrderStatus.OP_RECEIVE, from, to, "用户确认收货", null);
        log.info("Order received: orderId={}", orderId);
        if (order.getShippingAddressId() != null) {
            shippingAddressService.markDisabledIfReferenced(order.getShippingAddressId(), userId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Long recordAfterSaleIntent(Long orderId, Long userId, String source) {
        OrderMain order = orderMainMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalArgumentException("仅已完成订单可申请售后：当前 " + OrderStatus.text(order.getStatus()));
        }
        LocalDateTime completedAt = order.getCompletedAt();
        if (completedAt != null && completedAt.plusDays(7).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("已超过7天售后申请期限");
        }
        AfterSaleIntent intent = new AfterSaleIntent();
        intent.setOrderId(orderId);
        intent.setOrderNo(order.getOrderNo());
        intent.setUserId(userId);
        intent.setIntentSource(source == null ? "DETAIL" : source.toUpperCase());
        afterSaleIntentMapper.insert(intent);
        writeLog(orderId, order.getOrderNo(), userId, resolveUserName(userId), "user",
                OrderStatus.OP_AFTER_SALE_INTENT, order.getStatus(), order.getStatus(),
                "用户点击申请售后，来源=" + intent.getIntentSource(), null);
        log.info("After-sale intent recorded: orderId={}, userId={}", orderId, userId);
        return intent.getId();
    }

    public List<OrderItem> listItems(Long orderId) {
        return orderItemMapper.selectByOrderId(orderId);
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generateShareToken(Long orderId, Long userId) {
        OrderMain order = orderMainMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("订单不存在");
        }
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        OrderShareToken shareToken = new OrderShareToken();
        shareToken.setOrderId(orderId);
        shareToken.setToken(token);
        shareToken.setExpiresAt(LocalDateTime.now().plusHours(24));
        orderShareTokenMapper.insert(shareToken);
        return token;
    }

    public Map<String, Object> getSharedOrder(String token) {
        OrderShareToken shareToken = orderShareTokenMapper.selectByToken(token);
        if (shareToken == null) {
            throw new IllegalArgumentException("分享链接无效");
        }
        if (shareToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("分享链接已过期");
        }
        OrderMain order = orderMainMapper.selectById(shareToken.getOrderId());
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());

        String maskedPhone = maskPhone(order.getReceiverPhone());
        String maskedAddress = maskAddress(order.getReceiverAddress());

        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("status", order.getStatus());
        result.put("statusText", OrderStatus.text(order.getStatus()));
        result.put("totalAmount", order.getTotalAmount());
        result.put("receiverName", order.getReceiverName());
        result.put("receiverPhone", maskedPhone);
        result.put("receiverAddress", maskedAddress);
        result.put("createdAt", formatDateTime(order.getCreatedAt()));
        result.put("shippedAt", formatDateTime(order.getShippedAt()));
        result.put("completedAt", formatDateTime(order.getCompletedAt()));
        result.put("items", items.stream().map(item -> {
            Map<String, Object> m = new HashMap<>();
            m.put("productName", item.getProductName());
            m.put("price", item.getPrice());
            m.put("quantity", item.getQuantity());
            m.put("totalAmount", item.getTotalAmount());
            return m;
        }).toList());
        result.put("timeline", buildTimeline(order));
        return result;
    }

    public List<OrderOperationLog> listOperationLogs(Long orderId) {
        return orderOperationLogMapper.selectByOrderId(orderId);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskAddress(String address) {
        if (address == null) return "***";
        int len = address.length();
        if (len <= 6) return address.substring(0, Math.min(3, len)) + "***";
        return address.substring(0, len - 3) + "***";
    }

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return null;
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private List<Map<String, Object>> buildTimeline(OrderMain order) {
        List<Map<String, Object>> timeline = new java.util.ArrayList<>();
        if (order.getCreatedAt() != null) {
            timeline.add(Map.of("label", "下单", "time", formatDateTime(order.getCreatedAt())));
        }
        if (order.getStatus() >= 1 && order.getCreatedAt() != null) {
            List<OrderOperationLog> logs = orderOperationLogMapper.selectByOrderId(order.getId());
            logs.stream()
                .filter(l -> "PAY".equals(l.getOperation()))
                .findFirst()
                .ifPresent(l -> timeline.add(Map.of("label", "支付", "time", formatDateTime(l.getCreatedAt()))));
        }
        if (order.getStatus() >= 2 && order.getShippedAt() != null) {
            timeline.add(Map.of("label", "发货", "time", formatDateTime(order.getShippedAt())));
        }
        return timeline;
    }
}
