package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.config.SystemConstants;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.hmdp.config.RedisConstants.BLOG_LIKED_KEY;
import static com.hmdp.config.RedisConstants.Feed_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author jasper
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IFollowService followService;
    @Override
    public Result saveBlog(Blog blog) {
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        boolean isSuccess = save(blog);
        if (!isSuccess) {
            return Result.fail("failed to add fucking blog");
        }
//查询author的所有fans
        List<Follow> follows = followService.lambdaQuery().eq(Follow::getFollowUserId, user.getId()).list();
        for (Follow follow : follows) {
            Long userId = follow.getUserId();
//        推送blog id 给all fans   收件箱 zset
            String key = Feed_KEY +userId;
            stringRedisTemplate.opsForZSet().add(key,blog.getId().toString(),System.currentTimeMillis());
        }
//        返回id
        return Result.ok(blog.getId());
    }

    /**
     * 滚动分页
     * @param max
     * @param offset
     * @return
     */
    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
//        获取当前用户
        Long userId = UserHolder.getUser().getId();
        String key = Feed_KEY + userId;
//        查询收件箱 zrevrangebyscore key max min withscores byscore  limit offset count
//       max（first 当前时间戳 肯定是最大的分数） 上次查询的分数（时间戳）最小值
//       offset（第一次0）:查询出来的分数与最小值一样的个数
        Set<ZSetOperations.TypedTuple<String>> typedTuples
                = stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(
                key, 0, max, offset, 2
        );
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }
//        解析 blogId  score （时间戳） mintime上一次查询的最小时间戳
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0;
        int os = 1; //offset
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
//            id
            ids.add(Long.valueOf( typedTuple.getValue()));
//            score  时间戳
            long time = typedTuple.getScore().longValue();
            if(time == minTime){
                os++;
            }else {
                minTime = time;
                os = 1;
            }
        }
//        查询blog
        String idStr = StrUtil.join(",", ids);
        List<Blog> blogs = query().in("id", ids).last("order by field(id," + idStr + ")").list();
        for (Blog blog : blogs) {
            queryBlogUser(blog);
            isBlogLiked(blog);
        }
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setOffset(os);
        scrollResult.setMintime(minTime);
        return Result.ok(scrollResult);
    }


    private void isBlogLiked(Blog blog) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
//            用户没有登录 无需查看是否点赞
            return;
        }
        Long userId = user.getId();
        String key = BLOG_LIKED_KEY+blog.getId();
// 判断是否点赞
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score!=null);
    }
    @Override
    public Result queryBlogById(Long id) {
//        Blog blog = getById(id);
//        if (blog==null) {
//            return Result.fail("the blog don't fucking exist");
//        }
//        queryBlogUser(blog);
        Optional<Blog> blog = Optional.ofNullable(getById(id));
        blog.ifPresent( x ->{queryBlogUser(x);});
//        blog是否被点赞了
        isBlogLiked(blog.get());
        return Result.ok(blog);
    }


    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            this.isBlogLiked(blog);
            this.queryBlogUser(blog);
        });
        return Result.ok(records);
    }

    @Override
    public Result likeBlog(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key = BLOG_LIKED_KEY+id;
// 判断是否点赞
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if(score == null){
            //        没有可以点赞
            // 修改点赞数量
            boolean isSuccess = update().setSql("liked = liked +1 ").eq("id", id).update();
//        保存用户到 redis zSet  zadd key value score
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().add(key,userId.toString(),System.currentTimeMillis());
            }
        }else {
//        不可以
//        取消点赞
            boolean isSuccess = update().setSql("liked = liked -1 ").eq("id", id).update();
//        将用户从redis移除
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().remove(key,userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        String key = BLOG_LIKED_KEY+id;
        Set<String> top5Set = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top5Set == null || top5Set.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = top5Set.stream().map(Long::valueOf).collect(Collectors.toList());
//        数据库的 select * from in(1,5) 返回 5 1
//        String idStrOrder = StrUtil.join(",", ids);
//        保护用户隐私
//        order by field(id,5,1)
//        List<UserDTO> userDTOS = userService.query().in("id",ids).
//                last("order by field(id,"+idStrOrder+")").list()
//                .stream().
//                map(user -> BeanUtil.copyProperties(user, UserDTO.class)).
//                collect(Collectors.toList());
        List<UserDTO> userDTOS = userService.listByIds(ids).stream().
                map(user -> BeanUtil.copyProperties(user, UserDTO.class)).
                collect(Collectors.toList());
        Collections.reverse(userDTOS);
        return Result.ok(userDTOS);
    }




    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
