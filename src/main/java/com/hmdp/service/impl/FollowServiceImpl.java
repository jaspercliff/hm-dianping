package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 关注功能
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private IUserService userService;
    @Override
    public Result follow(Long followedUserId, Boolean isFollow) {
        Long userId = UserHolder.getUser().getId();
        if(isFollow){
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followedUserId);
            save(follow);
        }else{
            LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<Follow>().
                    eq(Follow::getUserId, userId).
                    eq(Follow::getFollowUserId, followedUserId);
            remove(wrapper);
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followedUserId) {
        Long userId = UserHolder.getUser().getId();
        Integer count = lambdaQuery().eq(Follow::getUserId, userId).
                eq(Follow::getFollowUserId, followedUserId)
                .count();
        return Result.ok(count > 0);
    }

    @Override
    public Result commonFollow(Long bloggerId) {
        Long userId = UserHolder.getUser().getId();
        List<Follow> follows = getBaseMapper().queryCommonFollow(userId, bloggerId);
        if (follows.size()==0) {
            return Result.ok();
        }
        ArrayList<Long> list = new ArrayList<>();
        for (Follow follow : follows) {
            Long followUserId = follow.getFollowUserId();
            list.add(followUserId);
        }
        List<UserDTO> users = userService.listByIds(list).stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(users);
    }
}
