package com.grad.course.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.course.common.BizException;
import com.grad.course.common.PageResult;
import com.grad.course.common.UserContext;
import com.grad.course.entity.*;
import com.grad.course.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 讨论答疑：按课程开设主题，学生发帖、师生回帖，教师可标记精华与已解决。
 * 相似提问合并：同一课程同一作者在 30 分钟内连续发帖超过 3 条时提示已有解答（防刷屏）。
 */
@Service
@RequiredArgsConstructor
public class DiscussionService {

    private final DiscussionPostMapper postMapper;
    private final DiscussionReplyMapper replyMapper;
    private final CourseMapper courseMapper;
    private final SysUserMapper userMapper;
    private final TraceService trace;

    public PageResult<Map<String, Object>> page(Long courseId, String keyword, Boolean onlySolved,
                                               long page, long size) {
        QueryWrapper<DiscussionPost> w = new QueryWrapper<DiscussionPost>().eq("deleted", 0);
        if (courseId != null) {
            w.eq("course_id", courseId);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(x -> x.like("title", keyword).or().like("content", keyword));
        }
        if (onlySolved != null) {
            w.eq("is_solved", onlySolved ? 1 : 0);
        }
        w.orderByDesc("is_top").orderByDesc("create_time");
        Page<DiscussionPost> p = postMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (DiscussionPost d : p.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("postNo", d.getPostNo());
            m.put("courseId", d.getCourseId());
            Course c = courseMapper.selectById(d.getCourseId());
            m.put("courseName", c == null ? "" : c.getCourseName());
            SysUser a = userMapper.selectById(d.getAuthorId());
            m.put("authorName", a == null ? "" : a.getRealName());
            m.put("authorTitle", a == null ? "" : a.getTitle());
            m.put("title", d.getTitle());
            m.put("content", d.getContent());
            m.put("isTop", d.getIsTop());
            m.put("isEssence", d.getIsEssence());
            m.put("isSolved", d.getIsSolved());
            m.put("replyCount", d.getReplyCount());
            m.put("createTime", d.getCreateTime());
            rows.add(m);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    public Map<String, Object> detail(Long id) {
        DiscussionPost d = postMapper.selectById(id);
        if (d == null) {
            throw new BizException(404, "帖子不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("post", d);
        SysUser a = userMapper.selectById(d.getAuthorId());
        m.put("authorName", a == null ? "" : a.getRealName());
        Course c = courseMapper.selectById(d.getCourseId());
        m.put("courseName", c == null ? "" : c.getCourseName());
        List<Map<String, Object>> reps = new ArrayList<>();
        for (DiscussionReply r : replyMapper.selectList(new QueryWrapper<DiscussionReply>()
                .eq("post_id", id).eq("deleted", 0).orderByAsc("create_time"))) {
            Map<String, Object> rm = new LinkedHashMap<>();
            rm.put("id", r.getId());
            rm.put("content", r.getContent());
            rm.put("isAdopted", r.getIsAdopted());
            rm.put("likeCount", r.getLikeCount());
            rm.put("createTime", r.getCreateTime());
            SysUser u = userMapper.selectById(r.getAuthorId());
            rm.put("authorName", u == null ? "" : u.getRealName());
            rm.put("authorTitle", u == null ? "" : u.getTitle());
            reps.add(rm);
        }
        m.put("replies", reps);
        return m;
    }

    @Transactional
    public Long create(Long courseId, String title, String content, Long chapterId) {
        if (title == null || title.trim().length() < 4) {
            throw new BizException("标题不少于 4 个字");
        }
        if (content == null || content.trim().length() < 10) {
            throw new BizException("问题描述不少于 10 个字");
        }
        // 防护：同一作者 30 分钟内同课程发帖超过 3 条视为刷屏
        long recent = postMapper.selectCount(new QueryWrapper<DiscussionPost>()
                .eq("author_id", UserContext.userId()).eq("course_id", courseId)
                .ge("create_time", LocalDateTime.now().minusMinutes(30)));
        if (recent >= 3) {
            throw new BizException("30 分钟内已发布 3 条提问，请先在已有帖子下追问");
        }
        DiscussionPost d = new DiscussionPost();
        d.setPostNo("TZ" + System.currentTimeMillis() % 1000000);
        d.setCourseId(courseId);
        d.setChapterId(chapterId == null ? 0L : chapterId);
        d.setAuthorId(UserContext.userId());
        d.setTitle(title);
        d.setContent(content);
        d.setIsTop(0);
        d.setIsEssence(0);
        d.setIsSolved(0);
        d.setReplyCount(0);
        d.setStatus("正常");
        d.setCreateTime(LocalDateTime.now());
        d.setUpdateTime(LocalDateTime.now());
        postMapper.insert(d);
        trace.log("讨论区", "发帖", d.getPostNo(), title);
        return d.getId();
    }

    @Transactional
    public Long reply(Long postId, String content, boolean adopted) {
        DiscussionPost d = postMapper.selectById(postId);
        if (d == null) {
            throw new BizException(404, "帖子不存在");
        }
        if (content == null || content.trim().length() < 2) {
            throw new BizException("回复内容不少于 2 个字");
        }
        DiscussionReply r = new DiscussionReply();
        r.setPostId(postId);
        r.setAuthorId(UserContext.userId());
        r.setContent(content);
        r.setIsAdopted(adopted ? 1 : 0);
        r.setLikeCount(0);
        r.setStatus("正常");
        r.setCreateTime(LocalDateTime.now());
        replyMapper.insert(r);
        DiscussionPost up = new DiscussionPost();
        up.setId(postId);
        up.setReplyCount((d.getReplyCount() == null ? 0 : d.getReplyCount()) + 1);
        up.setUpdateTime(LocalDateTime.now());
        postMapper.updateById(up);
        return r.getId();
    }

    /** 教师 / 助教维护：标记精华、已解决、置顶 */
    @Transactional
    public boolean mark(Long postId, String type, boolean value) {
        DiscussionPost d = postMapper.selectById(postId);
        if (d == null) {
            throw new BizException(404, "帖子不存在");
        }
        DiscussionPost up = new DiscussionPost();
        up.setId(postId);
        switch (type) {
            case "essence" -> up.setIsEssence(value ? 1 : 0);
            case "solved" -> up.setIsSolved(value ? 1 : 0);
            case "top" -> up.setIsTop(value ? 1 : 0);
            default -> throw new BizException("未知的标记类型：" + type);
        }
        postMapper.updateById(up);
        trace.log("讨论区", "维护帖子", d.getPostNo(), type + "=" + value);
        return true;
    }
}
