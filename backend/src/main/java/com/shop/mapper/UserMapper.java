package com.shop.mapper;

import com.shop.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper {

    int insert(User user);

    User selectById(@Param("id") Long id);

    User selectByUsername(@Param("username") String username);

    List<User> selectAll();

    int updateById(User user);
}
