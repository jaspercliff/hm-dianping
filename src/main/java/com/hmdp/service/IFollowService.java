package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;

public interface IFollowService extends IService<Follow> {

    Result follow(Long followedUserId, Boolean isFollow);

    Result isFollow(Long followedUserId);

    Result commonFollow(Long bloggerId);
}
