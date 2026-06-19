package com.shop.mapper;

import com.shop.entity.BrowseHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BrowseHistoryMapper {

    int insertOrUpdate(@Param("userId") Long userId,
                       @Param("productId") Long productId,
                       @Param("categoryId") Long categoryId);

    List<BrowseHistory> selectByUserAndCategory(@Param("userId") Long userId,
                                                @Param("categoryId") Long categoryId,
                                                @Param("limit") int limit);

    List<Long> selectRecentProductIdsByUser(@Param("userId") Long userId,
                                            @Param("limit") int limit);
}
