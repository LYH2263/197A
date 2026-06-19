package com.shop.service;

import com.shop.dto.LoginRequest;
import com.shop.dto.RegisterRequest;
import com.shop.dto.UserVO;
import com.shop.entity.User;
import com.shop.mapper.UserMapper;
import com.shop.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

/**
 * AuthService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("认证服务单元测试")
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("123456");
        registerRequest.setNickname("测试用户");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("123456");

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("encoded");
        user.setNickname("测试用户");
        user.setStatus(1);
        user.setRole("user");
    }

    @Test
    @DisplayName("注册-用户名已存在应抛出异常")
    void register_duplicateUsername_throws() {
        when(userMapper.selectByUsername("testuser")).thenReturn(user);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户名已存在");
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("注册-成功应返回 token")
    void register_success_returnsToken() {
        when(userMapper.selectByUsername("testuser")).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn("encoded");
        doAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return null;
        }).when(userMapper).insert(any(User.class));
        when(jwtService.generateToken(1L)).thenReturn("jwt-token");

        String token = authService.register(registerRequest);

        assertThat(token).isEqualTo("jwt-token");
        verify(userMapper).insert(any(User.class));
        verify(jwtService).generateToken(1L);
    }

    @Test
    @DisplayName("登录-用户不存在应抛出异常")
    void login_userNotFound_throws() {
        when(userMapper.selectByUsername("testuser")).thenReturn(null);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户名或密码错误");
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("登录-密码错误应抛出异常")
    void login_wrongPassword_throws() {
        when(userMapper.selectByUsername("testuser")).thenReturn(user);
        when(passwordEncoder.matches("123456", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户名或密码错误");
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("登录-账号禁用应抛出异常")
    void login_disabledUser_throws() {
        user.setStatus(0);
        when(userMapper.selectByUsername("testuser")).thenReturn(user);
        when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("账号已被禁用");
    }

    @Test
    @DisplayName("登录-成功应返回 token")
    void login_success_returnsToken() {
        when(userMapper.selectByUsername("testuser")).thenReturn(user);
        when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);
        when(jwtService.generateToken(1L)).thenReturn("jwt-token");

        String token = authService.login(loginRequest);

        assertThat(token).isEqualTo("jwt-token");
        verify(jwtService).generateToken(1L);
    }

    @Test
    @DisplayName("获取当前用户-用户不存在返回 null")
    void getCurrentUser_notFound_returnsNull() {
        when(userMapper.selectById(1L)).thenReturn(null);

        UserVO vo = authService.getCurrentUser(1L);

        assertThat(vo).isNull();
    }

    @Test
    @DisplayName("获取当前用户-成功返回 UserVO")
    void getCurrentUser_success_returnsVO() {
        when(userMapper.selectById(1L)).thenReturn(user);

        UserVO vo = authService.getCurrentUser(1L);

        assertThat(vo).isNotNull();
        assertThat(vo.getUsername()).isEqualTo("testuser");
        assertThat(vo.getNickname()).isEqualTo("测试用户");
        assertThat(vo.getRole()).isEqualTo("user");
    }
}
