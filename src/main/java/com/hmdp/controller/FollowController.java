package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author jasper
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followedUserId,@PathVariable("isFollow")Boolean isFollow ) {
        return followService.follow(followedUserId,isFollow);
    }

    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long followedUserId){
        return followService.isFollow(followedUserId);
    }

    @GetMapping("/common/{id}")
    public Result commonFollow(@PathVariable("id") Long bloggerId){
        return followService.commonFollow(bloggerId);
    }
}
