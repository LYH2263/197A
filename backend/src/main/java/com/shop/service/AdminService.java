package com.shop.service;

import com.shop.dto.ReviewVO;
import com.shop.dto.UserVO;
import com.shop.entity.User;
import com.shop.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理员服务：用户管理、评价管理
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserMapper userMapper;
    private final ReviewService reviewService;

    /** 用户列表（管理员） */
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

    /** 更新用户（状态、角色） */
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

    /** 全部评价列表（管理员） */
    public List<ReviewVO> listAllReviews() {
        return reviewService.listAll();
    }

    /** 删除评价（管理员） */
    public void deleteReview(Long id) {
        reviewService.deleteById(id);
    }
}
