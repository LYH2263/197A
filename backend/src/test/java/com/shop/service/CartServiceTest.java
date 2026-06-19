package com.shop.service;

import com.shop.dto.CartItemVO;
import com.shop.entity.CartItem;
import com.shop.entity.Product;
import com.shop.mapper.CartItemMapper;
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
import static org.mockito.Mockito.*;

/**
 * CartService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("购物车服务单元测试")
class CartServiceTest {

    @Mock
    private CartItemMapper cartItemMapper;
    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private CartService cartService;

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 10L;
    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("测试商品");
        product.setPrice(new BigDecimal("99.00"));
        product.setStock(100);
    }

    @Test
    @DisplayName("添加购物车-商品不存在应抛出异常")
    void add_productNotFound_throws() {
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(null);

        assertThatThrownBy(() -> cartService.add(USER_ID, PRODUCT_ID, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("商品不存在");
        verify(cartItemMapper, never()).insert(any());
    }

    @Test
    @DisplayName("添加购物车-库存不足应抛出异常")
    void add_insufficientStock_throws() {
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

        assertThatThrownBy(() -> cartService.add(USER_ID, PRODUCT_ID, 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("库存不足");
    }

    @Test
    @DisplayName("添加购物车-新商品应调用 insert")
    void add_newItem_callsInsert() {
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);
        when(cartItemMapper.selectByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(null);

        cartService.add(USER_ID, PRODUCT_ID, 2);

        verify(cartItemMapper).insert(argThat(item ->
                item.getUserId().equals(USER_ID) && item.getProductId().equals(PRODUCT_ID) && item.getQuantity() == 2
        ));
    }

    @Test
    @DisplayName("添加购物车-已存在应更新数量")
    void add_existingItem_updatesQuantity() {
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);
        CartItem existing = new CartItem();
        existing.setId(1L);
        existing.setUserId(USER_ID);
        existing.setProductId(PRODUCT_ID);
        existing.setQuantity(1);
        when(cartItemMapper.selectByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(existing);

        cartService.add(USER_ID, PRODUCT_ID, 3);

        verify(cartItemMapper).updateById(argThat(item -> item.getQuantity() == 4));
    }

    @Test
    @DisplayName("购物车列表-空列表返回空")
    void list_empty_returnsEmpty() {
        when(cartItemMapper.selectByUserId(USER_ID)).thenReturn(Collections.emptyList());

        List<CartItemVO> list = cartService.list(USER_ID);

        assertThat(list).isEmpty();
    }

    @Test
    @DisplayName("修改数量-购物车中无该商品应抛出异常")
    void updateQuantity_notInCart_throws() {
        when(cartItemMapper.selectByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(null);

        assertThatThrownBy(() -> cartService.updateQuantity(USER_ID, PRODUCT_ID, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("购物车中无该商品");
    }

    @Test
    @DisplayName("修改数量-超过库存应抛出异常")
    void updateQuantity_exceedsStock_throws() {
        CartItem item = new CartItem();
        item.setUserId(USER_ID);
        item.setProductId(PRODUCT_ID);
        item.setQuantity(1);
        when(cartItemMapper.selectByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(item);
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

        assertThatThrownBy(() -> cartService.updateQuantity(USER_ID, PRODUCT_ID, 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("库存不足");
    }
}
