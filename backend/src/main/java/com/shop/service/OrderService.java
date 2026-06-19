package com.shop.service;

import com.shop.dto.OrderCreateRequest;
import com.shop.entity.CartItem;
import com.shop.entity.OrderItem;
import com.shop.entity.OrderMain;
import com.shop.entity.Product;
import com.shop.entity.ShippingAddress;
import com.shop.mapper.CartItemMapper;
import com.shop.mapper.OrderItemMapper;
import com.shop.mapper.OrderMainMapper;
import com.shop.mapper.ProductMapper;
import com.shop.mapper.ShippingAddressMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final ShippingAddressMapper shippingAddressMapper;

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
        order.setStatus(0);
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
        log.info("Order created: orderNo={}, userId={}", orderNo, userId);
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
        if (order.getStatus() != 0) {
            throw new IllegalArgumentException("订单状态不允许支付");
        }
        orderMainMapper.updateStatus(orderId, 1);
        log.info("Order paid: orderId={}", orderId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, Long userId) {
        OrderMain order = orderMainMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new IllegalArgumentException("仅待付款订单可取消");
        }
        orderMainMapper.updateStatus(orderId, 4);
        log.info("Order cancelled: orderId={}", orderId);
    }

    public List<OrderItem> listItems(Long orderId) {
        return orderItemMapper.selectByOrderId(orderId);
    }
}
