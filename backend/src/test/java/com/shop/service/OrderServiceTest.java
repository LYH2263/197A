package com.shop.service;

import com.shop.dto.OrderCreateRequest;
import com.shop.entity.CartItem;
import com.shop.entity.OrderMain;
import com.shop.entity.Product;
import com.shop.mapper.CartItemMapper;
import com.shop.mapper.OrderItemMapper;
import com.shop.mapper.OrderMainMapper;
import com.shop.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * OrderService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("订单服务单元测试")
class OrderServiceTest {

    @Mock
    private OrderMainMapper orderMainMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private CartItemMapper cartItemMapper;
    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private OrderService orderService;

    private static final Long USER_ID = 1L;
    private static final Long ORDER_ID = 100L;
    private static final Long OTHER_USER_ID = 2L;
    private OrderCreateRequest createRequest;
    private OrderMain order;
    private Product product;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        createRequest = new OrderCreateRequest();
        createRequest.setReceiverName("张三");
        createRequest.setReceiverPhone("13800138000");
        createRequest.setReceiverAddress("北京市朝阳区");

        order = new OrderMain();
        order.setId(ORDER_ID);
        order.setUserId(USER_ID);
        order.setStatus(0);
        order.setOrderNo("O20240101120000001");

        product = new Product();
        product.setId(10L);
        product.setName("测试商品");
        product.setPrice(new BigDecimal("99.00"));
        product.setStock(100);
        product.setMainImage("http://img.example.com/1.jpg");

        cartItem = new CartItem();
        cartItem.setUserId(USER_ID);
        cartItem.setProductId(10L);
        cartItem.setQuantity(2);
        cartItem.setChecked(1);
    }

    @Test
    @DisplayName("创建订单-未勾选购物车应抛出异常")
    void create_emptyCheckedCart_throws() {
        when(cartItemMapper.selectCheckedByUserId(USER_ID)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> orderService.create(USER_ID, createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("请先勾选要结算的商品");
        verify(orderMainMapper, never()).insert(any());
    }

    @Test
    @DisplayName("支付-订单不存在应抛出异常")
    void pay_orderNotFound_throws() {
        when(orderMainMapper.selectById(ORDER_ID)).thenReturn(null);

        assertThatThrownBy(() -> orderService.pay(ORDER_ID, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("订单不存在");
        verify(orderMainMapper, never()).updateStatus(anyLong(), any());
    }

    @Test
    @DisplayName("支付-非订单所属用户应抛出异常")
    void pay_wrongUser_throws() {
        when(orderMainMapper.selectById(ORDER_ID)).thenReturn(order);

        assertThatThrownBy(() -> orderService.pay(ORDER_ID, OTHER_USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("订单不存在");
    }

    @Test
    @DisplayName("支付-订单状态非待付款应抛出异常")
    void pay_statusNotPending_throws() {
        order.setStatus(1);
        when(orderMainMapper.selectById(ORDER_ID)).thenReturn(order);

        assertThatThrownBy(() -> orderService.pay(ORDER_ID, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("订单状态不允许支付");
    }

    @Test
    @DisplayName("支付-成功应更新状态为已付款")
    void pay_success_updatesStatus() {
        when(orderMainMapper.selectById(ORDER_ID)).thenReturn(order);

        orderService.pay(ORDER_ID, USER_ID);

        verify(orderMainMapper).updateStatus(ORDER_ID, 1);
    }

    @Test
    @DisplayName("取消订单-仅待付款可取消")
    void cancel_statusNotPending_throws() {
        order.setStatus(1);
        when(orderMainMapper.selectById(ORDER_ID)).thenReturn(order);

        assertThatThrownBy(() -> orderService.cancel(ORDER_ID, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("仅待付款订单可取消");
    }

    @Test
    @DisplayName("取消订单-成功应更新状态为已取消")
    void cancel_success_updatesStatus() {
        when(orderMainMapper.selectById(ORDER_ID)).thenReturn(order);

        orderService.cancel(ORDER_ID, USER_ID);

        verify(orderMainMapper).updateStatus(ORDER_ID, 4);
    }

    @Test
    @DisplayName("根据ID获取订单-非所属用户返回 null")
    void getById_wrongUser_returnsNull() {
        when(orderMainMapper.selectById(ORDER_ID)).thenReturn(order);

        OrderMain result = orderService.getById(ORDER_ID, OTHER_USER_ID);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("根据ID获取订单-所属用户返回订单")
    void getById_owner_returnsOrder() {
        when(orderMainMapper.selectById(ORDER_ID)).thenReturn(order);

        OrderMain result = orderService.getById(ORDER_ID, USER_ID);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(ORDER_ID);
    }

    @Test
    @DisplayName("按用户查询订单列表")
    void listByUserId_returnsList() {
        List<OrderMain> orders = List.of(order);
        when(orderMainMapper.selectByUserId(USER_ID)).thenReturn(orders);

        List<OrderMain> result = orderService.listByUserId(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderNo()).isEqualTo("O20240101120000001");
    }
}
