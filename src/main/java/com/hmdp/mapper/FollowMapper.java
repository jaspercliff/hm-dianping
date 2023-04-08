package com.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.entity.Follow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author jasper
 * @since 2021-12-22
 */
public interface FollowMapper extends BaseMapper<Follow> {

    List<Follow> queryCommonFollow(@Param("userId") Long userId,@Param("bloggerId") Long bloggerId);
}
