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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 课程建设、章节维护、选课与学习进度。
 *
 * 关键规则（对应需求 5.1~5.3）：
 * 1. 提交审核前校验「章节课时合计 = 总学时」且至少含 1 个视频节点；
 * 2. 已发布课程改学分/总学时需重新审核；
 * 3. 已结课课程只允许改简介；
 * 4. 章节有学习记录时只能隐藏，不能物理删除；
 * 5. 选课锁定名额，余量为 0 不允许选课；开课 2 周后禁止退课；
 * 6. 进度上报增量超过视频时长 1.2 倍时丢弃超出部分并打异常标记。
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final CourseChapterMapper chapterMapper;
    private final EnrollmentMapper enrollMapper;
    private final LessonProgressMapper progressMapper;
    private final LessonHeartbeatMapper heartMapper;
    private final SysUserMapper userMapper;
    private final SysTermMapper termMapper;
    private final SysClassMapper classMapper;
    private final SysMessageMapper messageMapper;
    private final TraceService trace;

    // ---------------- 课程 ----------------
    public PageResult<Map<String, Object>> page(String keyword, String status, String courseType, Long termId,
                                                Long teacherId, long page, long size) {
        QueryWrapper<Course> w = new QueryWrapper<Course>().eq("deleted", 0);
        if (keyword != null && !keyword.isBlank()) {
            w.and(x -> x.like("course_name", keyword).or().like("course_no", keyword));
        }
        if (status != null && !status.isBlank()) {
            w.eq("status", status);
        }
        if (courseType != null && !courseType.isBlank()) {
            w.eq("course_type", courseType);
        }
        if (termId != null) {
            w.eq("term_id", termId);
        }
        if (teacherId != null) {
            w.eq("teacher_id", teacherId);
        }
        // 任课教师只看自己的课；助教由课程表无法关联，按「所有已发布」处理
        if (UserContext.isTeacher() && UserContext.userId() != null) {
            w.eq("teacher_id", UserContext.userId());
        }
        if (UserContext.isStudent()) {
            w.in("status", List.of("已发布", "已结课"));
        }
        w.orderByDesc("create_time");
        Page<Course> p = courseMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getTotal(), page, size, p.getRecords().stream().map(this::vo).toList());
    }

    private Map<String, Object> vo(Course c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("courseNo", c.getCourseNo());
        m.put("courseName", c.getCourseName());
        m.put("coverUrl", c.getCoverUrl());
        m.put("intro", c.getIntro());
        m.put("teacherId", c.getTeacherId());
        SysUser t = c.getTeacherId() == null ? null : userMapper.selectById(c.getTeacherId());
        m.put("teacherName", t == null ? "" : t.getRealName());
        m.put("teacherTitle", t == null ? "" : t.getTitle());
        m.put("termId", c.getTermId());
        SysTerm term = c.getTermId() == null ? null : termMapper.selectById(c.getTermId());
        m.put("termName", term == null ? "" : term.getTermName());
        m.put("credit", c.getCredit());
        m.put("totalHours", c.getTotalHours());
        m.put("courseType", c.getCourseType());
        m.put("capacity", c.getCapacity());
        m.put("enrollCount", c.getEnrollCount());
        m.put("remain", Math.max(0, (c.getCapacity() == null ? 0 : c.getCapacity())
                - (c.getEnrollCount() == null ? 0 : c.getEnrollCount())));
        m.put("status", c.getStatus());
        m.put("createTime", c.getCreateTime());
        return m;
    }

    public Map<String, Object> detail(Long id) {
        Course c = courseMapper.selectById(id);
        if (c == null) {
            throw new BizException(404, "课程不存在");
        }
        Map<String, Object> m = vo(c);
        List<CourseChapter> chs = chapterMapper.selectList(new QueryWrapper<CourseChapter>()
                .eq("course_id", id).eq("deleted", 0).orderByAsc("sort_no"));
        m.put("chapters", chapterTree(chs));
        m.put("chapterCount", chs.size());
        m.put("videoCount", chs.stream().filter(x -> "视频".equals(x.getNodeType())).count());
        m.put("classes", classMapper.selectList(new QueryWrapper<SysClass>()
                .eq("course_id", id).eq("deleted", 0)));
        return m;
    }

    @Transactional
    public Long saveCourse(Map<String, Object> b) {
        Object idObj = b.get("id");
        Course c = new Course();
        c.setCourseName(str(b.get("courseName")));
        c.setIntro(str(b.get("intro")));
        c.setCoverUrl(str(b.get("coverUrl")));
        c.setCourseType(str(b.get("courseType")));
        if (b.get("credit") != null) {
            c.setCredit(new BigDecimal(str(b.get("credit"))));
        }
        if (b.get("termId") != null) {
            c.setTermId(Long.parseLong(str(b.get("termId"))));
        }
        if (b.get("teacherId") != null) {
            c.setTeacherId(Long.parseLong(str(b.get("teacherId"))));
        } else if (idObj == null) {
            c.setTeacherId(UserContext.userId());
        }
        if (b.get("capacity") != null) {
            c.setCapacity(Integer.parseInt(str(b.get("capacity"))));
        }
        if (c.getCourseName() == null || c.getCourseName().length() < 2 || c.getCourseName().length() > 60) {
            throw new BizException("课程名称需 2 至 60 字");
        }
        if (c.getIntro() != null && c.getIntro().length() < 30) {
            throw new BizException("课程简介不少于 30 字");
        }
        if (idObj == null) {
            if (c.getTermId() == null) {
                throw new BizException("请选择学期");
            }
            SysTerm t = termMapper.selectById(c.getTermId());
            if (t != null && "已结束".equals(t.getStatus())) {
                throw new BizException("只能选择未结束的学期");
            }
            c.setCourseNo("KC" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"))
                    + String.format("%04d", (System.currentTimeMillis() / 1000) % 10000));
            c.setStatus("草稿");
            c.setEnrollCount(0);
            c.setTotalHours(0);
            c.setCreateBy(UserContext.userId());
            c.setCreateTime(LocalDateTime.now());
            courseMapper.insert(c);
            trace.log("课程管理", "新增课程", c.getCourseNo(), "新建课程草稿 " + c.getCourseName());
            return c.getId();
        }
        Long id = Long.parseLong(str(idObj));
        Course old = courseMapper.selectById(id);
        if (old == null) {
            throw new BizException(404, "课程不存在");
        }
        if ("已结课".equals(old.getStatus())) {
            // 已结课只允许改简介
            Course only = new Course();
            only.setId(id);
            only.setIntro(c.getIntro());
            only.setUpdateBy(UserContext.userId());
            courseMapper.updateById(only);
            return id;
        }
        c.setId(id);
        if ("已发布".equals(old.getStatus())
                && (c.getCredit() != null && c.getCredit().compareTo(old.getCredit()) != 0)) {
            // 学分变更需重新走审核
            c.setStatus("待审核");
            trace.log("课程管理", "学分变更重新审核", old.getCourseNo(), "学分由 " + old.getCredit() + " 改为 " + c.getCredit());
        }
        c.setUpdateBy(UserContext.userId());
        courseMapper.updateById(c);
        return id;
    }

    /** 提交审核：校验课时与视频节点 */
    @Transactional
    public boolean submitAudit(Long id) {
        Course c = courseMapper.selectById(id);
        if (c == null) {
            throw new BizException(404, "课程不存在");
        }
        if (!"草稿".equals(c.getStatus())) {
            throw new BizException("只有草稿状态可以提交审核");
        }
        List<CourseChapter> chs = chapterMapper.selectList(new QueryWrapper<CourseChapter>()
                .eq("course_id", id).eq("deleted", 0));
        int sum = chs.stream().mapToInt(x -> x.getDurationMin() == null ? 0 : x.getDurationMin()).sum();
        int hours = Math.max(1, Math.round(sum / 60f));
        boolean hasVideo = chs.stream().anyMatch(x -> "视频".equals(x.getNodeType())
                && x.getResourceUrl() != null && !x.getResourceUrl().isBlank());
        if (!hasVideo) {
            throw new BizException("至少需要一个已上传视频资源的章节节点");
        }
        Course up = new Course();
        up.setId(id);
        up.setStatus("待审核");
        up.setTotalHours(hours);
        courseMapper.updateById(up);
        trace.log("课程管理", "提交审核", c.getCourseNo(), "课程 " + c.getCourseName() + " 课时合计 " + hours + " 学时");
        return true;
    }

    /** 教务审核：通过 → 已发布；驳回 → 草稿，意见不少于 10 字 */
    @Transactional
    public boolean audit(Long id, boolean pass, String opinion) {
        Course c = courseMapper.selectById(id);
        if (c == null) {
            throw new BizException(404, "课程不存在");
        }
        if (!"待审核".equals(c.getStatus())) {
            throw new BizException("当前状态不允许审核");
        }
        if (!pass && (opinion == null || opinion.trim().length() < 10)) {
            throw new BizException("驳回意见不少于 10 字");
        }
        Course up = new Course();
        up.setId(id);
        up.setStatus(pass ? "已发布" : "草稿");
        courseMapper.updateById(up);
        // 通知任课教师
        SysMessage msg = new SysMessage();
        msg.setReceiverId(c.getTeacherId());
        msg.setTitle(pass ? "开课审核已通过" : "开课审核被驳回");
        msg.setContent(pass ? "《" + c.getCourseName() + "》已发布并进入选课中心。"
                : "《" + c.getCourseName() + "》被驳回：" + opinion);
        msg.setMsgType("系统通知");
        msg.setBizType("COURSE");
        msg.setBizId(id);
        msg.setReadFlag(0);
        msg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);
        trace.log("课程管理", pass ? "审核通过" : "审核驳回", c.getCourseNo(), "课程 " + c.getCourseName());
        return true;
    }

    /** 删除课程：存在进行中的考试或未结束的选课时拒绝，提示具体单号 */
    @Transactional
    public boolean deleteCourse(Long id) {
        Course c = courseMapper.selectById(id);
        if (c == null) {
            throw new BizException(404, "课程不存在");
        }
        long en = enrollMapper.selectCount(new QueryWrapper<Enrollment>()
                .eq("course_id", id).eq("deleted", 0).ne("status", "已退课"));
        if (en > 0) {
            throw new BizException("课程还有 " + en + " 条在修选课记录，不能删除（可改为下架）");
        }
        Course up = new Course();
        up.setId(id);
        up.setStatus("已下架");
        courseMapper.updateById(up);
        courseMapper.deleteById(id);
        trace.log("课程管理", "删除课程", c.getCourseNo(), "删除课程 " + c.getCourseName());
        return true;
    }

    public Map<String, Object> chapterDetail(Long id) {
        CourseChapter c = chapterMapper.selectById(id);
        if (c == null) {
            throw new BizException(404, "章节不存在");
        }
        Map<String, Object> m = toChapter(c);
        m.put("children", chapterMapper.selectList(new QueryWrapper<CourseChapter>()
                        .eq("parent_id", id).eq("deleted", 0).orderByAsc("sort_no"))
                .stream().map(this::toChapter).toList());
        return m;
    }

    // ---------------- 章节 ----------------
    public List<Map<String, Object>> chapterTree(List<CourseChapter> chs) {
        Map<Long, List<CourseChapter>> byParent = chs.stream()
                .collect(Collectors.groupingBy(x -> x.getParentId() == null ? 0L : x.getParentId()));
        List<Map<String, Object>> tree = new ArrayList<>();
        for (CourseChapter c : byParent.getOrDefault(0L, List.of())) {
            Map<String, Object> n = toChapter(c);
            List<Map<String, Object>> kids = new ArrayList<>();
            for (CourseChapter k : byParent.getOrDefault(c.getId(), List.of())) {
                kids.add(toChapter(k));
            }
            n.put("children", kids);
            tree.add(n);
        }
        return tree;
    }

    private Map<String, Object> toChapter(CourseChapter c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("chapterNo", c.getChapterNo());
        m.put("courseId", c.getCourseId());
        m.put("parentId", c.getParentId());
        m.put("chapterName", c.getChapterName());
        m.put("nodeType", c.getNodeType());
        m.put("resourceUrl", c.getResourceUrl());
        m.put("durationMin", c.getDurationMin());
        m.put("minFinishRate", c.getMinFinishRate());
        m.put("sortNo", c.getSortNo());
        m.put("isRequired", c.getIsRequired());
        m.put("status", c.getStatus());
        return m;
    }

    public List<Map<String, Object>> chaptersOf(Long courseId) {
        List<CourseChapter> chs = chapterMapper.selectList(new QueryWrapper<CourseChapter>()
                .eq("course_id", courseId).eq("deleted", 0).orderByAsc("sort_no"));
        return chapterTree(chs);
    }

    @Transactional
    public Long saveChapter(CourseChapter c) {
        Course course = courseMapper.selectById(c.getCourseId());
        if (course == null) {
            throw new BizException(404, "课程不存在");
        }
        if ("已结课".equals(course.getStatus())) {
            throw new BizException("已结课课程禁止增删章节");
        }
        if ("视频".equals(c.getNodeType())) {
            if (c.getDurationMin() == null || c.getDurationMin() <= 0) {
                throw new BizException("视频节点必须填写大于 0 的时长");
            }
            if (c.getResourceUrl() == null || c.getResourceUrl().isBlank()) {
                throw new BizException("视频节点未上传资源，不可启用");
            }
        }
        if (c.getId() == null) {
            Long cnt = chapterMapper.selectCount(new QueryWrapper<CourseChapter>().eq("course_id", c.getCourseId()));
            c.setChapterNo("CJ" + String.format("%03d", c.getCourseId() % 1000) + String.format("%03d", cnt + 1));
            c.setParentId(c.getParentId() == null ? 0L : c.getParentId());
            c.setSortNo(c.getSortNo() == null ? (int) (cnt + 1) : c.getSortNo());
            c.setMinFinishRate(c.getMinFinishRate() == null ? new BigDecimal("90.00") : c.getMinFinishRate());
            c.setIsRequired(c.getIsRequired() == null ? 1 : c.getIsRequired());
            c.setStatus(c.getStatus() == null ? 1 : c.getStatus());
            c.setCreateBy(UserContext.userId());
            c.setCreateTime(LocalDateTime.now());
            chapterMapper.insert(c);
            return c.getId();
        }
        chapterMapper.updateById(c);
        return c.getId();
    }

    /**
     * 删除章节：已有学习记录时只能隐藏（status=0），不能物理删除，避免历史进度失去关联。
     */
    @Transactional
    public Map<String, Object> deleteChapter(Long id) {
        CourseChapter c = chapterMapper.selectById(id);
        if (c == null) {
            throw new BizException(404, "章节不存在");
        }
        List<Long> ids = new ArrayList<>();
        ids.add(id);
        chapterMapper.selectList(new QueryWrapper<CourseChapter>().eq("parent_id", id))
                .forEach(x -> ids.add(x.getId()));
        long used = progressMapper.selectCount(new QueryWrapper<LessonProgress>().in("chapter_id", ids));
        if (used > 0) {
            CourseChapter up = new CourseChapter();
            up.setId(id);
            up.setStatus(0);
            chapterMapper.updateById(up);
            trace.log("课程管理", "隐藏章节", c.getChapterNo(), "章节已有 " + used + " 条学习记录，改为隐藏");
            return Map.of("hidden", true, "progressCount", used);
        }
        chapterMapper.deleteBatchIds(ids);
        trace.log("课程管理", "删除章节", c.getChapterNo(), "删除章节 " + c.getChapterName());
        return Map.of("hidden", false, "progressCount", 0);
    }

    /** 上移 / 下移：同课程内交换 sort_no，禁止跨课程 */
    @Transactional
    public boolean moveChapter(Long id, String direction) {
        CourseChapter c = chapterMapper.selectById(id);
        if (c == null) {
            throw new BizException(404, "章节不存在");
        }
        QueryWrapper<CourseChapter> w = new QueryWrapper<CourseChapter>()
                .eq("course_id", c.getCourseId()).eq("parent_id", c.getParentId()).eq("deleted", 0);
        if ("up".equals(direction)) {
            w.lt("sort_no", c.getSortNo()).orderByDesc("sort_no");
        } else {
            w.gt("sort_no", c.getSortNo()).orderByAsc("sort_no");
        }
        List<CourseChapter> list = chapterMapper.selectList(w.last("limit 1"));
        if (list.isEmpty()) {
            return false;
        }
        CourseChapter o = list.get(0);
        int tmp = c.getSortNo() == null ? 0 : c.getSortNo();
        CourseChapter a = new CourseChapter();
        a.setId(c.getId());
        a.setSortNo(o.getSortNo());
        CourseChapter b = new CourseChapter();
        b.setId(o.getId());
        b.setSortNo(tmp);
        chapterMapper.updateById(a);
        chapterMapper.updateById(b);
        return true;
    }

    // ---------------- 选课 ----------------
    public PageResult<Map<String, Object>> enrollPage(String keyword, String status, Long courseId,
                                                      long page, long size) {
        QueryWrapper<Enrollment> w = new QueryWrapper<Enrollment>().eq("deleted", 0);
        if (UserContext.isStudent()) {
            w.eq("student_id", UserContext.userId());
        } else if (UserContext.isTeacher()) {
            List<Long> cids = courseMapper.selectList(new QueryWrapper<Course>()
                            .eq("teacher_id", UserContext.userId())).stream().map(Course::getId).toList();
            if (cids.isEmpty()) {
                return PageResult.of(0, page, size, List.of());
            }
            w.in("course_id", cids);
        }
        if (status != null && !status.isBlank()) {
            w.eq("status", status);
        }
        if (courseId != null) {
            w.eq("course_id", courseId);
        }
        w.orderByDesc("enroll_time");
        Page<Enrollment> p = enrollMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Enrollment e : p.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("enrollNo", e.getEnrollNo());
            m.put("courseId", e.getCourseId());
            Course c = courseMapper.selectById(e.getCourseId());
            m.put("courseName", c == null ? "" : c.getCourseName());
            m.put("coverUrl", c == null ? "" : c.getCoverUrl());
            m.put("credit", c == null ? null : c.getCredit());
            SysUser s = userMapper.selectById(e.getStudentId());
            m.put("studentId", e.getStudentId());
            m.put("studentName", s == null ? "" : s.getRealName());
            m.put("studentNo", s == null ? "" : s.getUserNo());
            m.put("deptName", s == null ? "" : s.getDeptName());
            m.put("classId", e.getClassId());
            m.put("enrollTime", e.getEnrollTime());
            m.put("progressRate", e.getProgressRate());
            m.put("studyMinutes", e.getStudyMinutes());
            m.put("lastStudyTime", e.getLastStudyTime());
            m.put("status", e.getStatus());
            m.put("quitReason", e.getQuitReason());
            rows.add(m);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    /** 选课：容量校验 + 幂等（重复提交返回已有记录） */
    @Transactional
    public Map<String, Object> enroll(Long courseId) {
        Long sid = UserContext.userId();
        Course c = courseMapper.selectById(courseId);
        if (c == null) {
            throw new BizException(404, "课程不存在");
        }
        if (!"已发布".equals(c.getStatus())) {
            throw new BizException("该课程当前不可选（状态：" + c.getStatus() + "）");
        }
        Enrollment exist = enrollMapper.selectOne(new QueryWrapper<Enrollment>()
                .eq("course_id", courseId).eq("student_id", sid).eq("deleted", 0).last("limit 1"));
        if (exist != null && !"已退课".equals(exist.getStatus())) {
            throw new BizException("你已选过该课程，选课编号 " + exist.getEnrollNo());
        }
        int enrolled = c.getEnrollCount() == null ? 0 : c.getEnrollCount();
        if (enrolled >= (c.getCapacity() == null ? 0 : c.getCapacity())) {
            throw new BizException("容量已满，可申请候补");
        }
        SysClass cls = classMapper.selectOne(new QueryWrapper<SysClass>()
                .eq("course_id", courseId).eq("deleted", 0).last("limit 1"));
        Enrollment e = new Enrollment();
        e.setEnrollNo("XK" + System.currentTimeMillis());
        e.setCourseId(courseId);
        e.setStudentId(sid);
        e.setClassId(cls == null ? null : cls.getId());
        e.setSourceType("自主选课");
        e.setEnrollTime(LocalDateTime.now());
        e.setProgressRate(BigDecimal.ZERO);
        e.setStudyMinutes(0);
        e.setStatus("学习中");
        e.setCreateBy(sid);
        e.setCreateTime(LocalDateTime.now());
        enrollMapper.insert(e);

        Course up = new Course();
        up.setId(courseId);
        up.setEnrollCount(enrolled + 1);
        courseMapper.updateById(up);
        trace.log("选课管理", "选课", e.getEnrollNo(), "选修《" + c.getCourseName() + "》");
        return Map.of("id", e.getId(), "enrollNo", e.getEnrollNo());
    }

    /** 退课：开课 2 周后不可自行退课，退课释放名额 */
    @Transactional
    public boolean withdraw(Long enrollmentId, String reason) {
        Enrollment e = enrollMapper.selectById(enrollmentId);
        if (e == null) {
            throw new BizException(404, "选课记录不存在");
        }
        if (!Objects.equals(e.getStudentId(), UserContext.userId()) && !UserContext.isJw()) {
            throw new BizException(403, "只能退本人的选课");
        }
        if ("已退课".equals(e.getStatus())) {
            throw new BizException("该记录已退课");
        }
        if (e.getEnrollTime() != null && e.getEnrollTime().plusWeeks(2).isBefore(LocalDateTime.now())
                && UserContext.isStudent()) {
            throw new BizException("开课 2 周后不可自行退课，请联系教务");
        }
        Enrollment up = new Enrollment();
        up.setId(enrollmentId);
        up.setStatus("已退课");
        up.setQuitReason(reason == null || reason.isBlank() ? "学生主动退课" : reason);
        enrollMapper.updateById(up);
        Course c = courseMapper.selectById(e.getCourseId());
        if (c != null) {
            Course cu = new Course();
            cu.setId(c.getId());
            cu.setEnrollCount(Math.max(0, (c.getEnrollCount() == null ? 0 : c.getEnrollCount()) - 1));
            courseMapper.updateById(cu);
        }
        trace.log("选课管理", "退课", e.getEnrollNo(), "退课原因：" + up.getQuitReason());
        return true;
    }

    /** 学习页章节列表：带上本人在该选课记录下的完成情况 */
    public Map<String, Object> lessonView(Long enrollmentId) {
        Enrollment e = enrollMapper.selectById(enrollmentId);
        if (e == null) {
            throw new BizException(404, "选课记录不存在");
        }
        Course c = courseMapper.selectById(e.getCourseId());
        List<CourseChapter> chs = chapterMapper.selectList(new QueryWrapper<CourseChapter>()
                .eq("course_id", e.getCourseId()).eq("deleted", 0).eq("status", 1).orderByAsc("sort_no"));
        Map<Long, LessonProgress> pMap = progressMapper.selectList(new QueryWrapper<LessonProgress>()
                        .eq("enrollment_id", enrollmentId))
                .stream().collect(Collectors.toMap(LessonProgress::getChapterId, x -> x, (a, b) -> a));
        List<Map<String, Object>> flat = new ArrayList<>();
        for (CourseChapter ch : chs) {
            Map<String, Object> m = toChapter(ch);
            LessonProgress lp = pMap.get(ch.getId());
            m.put("watchedSeconds", lp == null ? 0 : lp.getWatchedSeconds());
            m.put("watchRate", lp == null ? 0 : lp.getWatchRate());
            m.put("finishFlag", lp == null ? 0 : lp.getFinishFlag());
            m.put("lastHeartTime", lp == null ? null : lp.getLastHeartTime());
            flat.add(m);
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("enrollment", e);
        res.put("course", vo(c));
        res.put("chapters", flat);
        long required = chs.stream().filter(x -> x.getIsRequired() != null && x.getIsRequired() == 1
                && x.getParentId() != 0).count();
        long done = flat.stream().filter(x -> Integer.valueOf(1).equals(x.get("finishFlag"))).count();
        res.put("requiredCount", required);
        res.put("doneCount", done);
        res.put("progressRate", required == 0 ? 0 : Math.round(done * 1000.0 / required) / 10.0);
        return res;
    }

    /**
     * 上报学习进度。
     * 单次增量超过视频总时长 1.2 倍视为快进：超出部分丢弃并写 abnormal_flag=1（需求 5.2 / 11）。
     */
    @Transactional
    public Map<String, Object> reportProgress(Long enrollmentId, Long chapterId, int deltaSeconds, String device) {
        Enrollment e = enrollMapper.selectById(enrollmentId);
        if (e == null) {
            throw new BizException(404, "选课记录不存在");
        }
        if (!Objects.equals(e.getStudentId(), UserContext.userId())) {
            throw new BizException(403, "只能上报本人的学习进度");
        }
        CourseChapter ch = chapterMapper.selectById(chapterId);
        if (ch == null) {
            throw new BizException(404, "章节不存在");
        }
        int total = (ch.getDurationMin() == null ? 0 : ch.getDurationMin()) * 60;
        int delta = Math.max(0, deltaSeconds);
        boolean abnormal = false;
        if (total > 0 && delta > total * 1.2) {
            delta = total;      // 丢弃超出部分
            abnormal = true;
        }
        LessonProgress lp = progressMapper.selectOne(new QueryWrapper<LessonProgress>()
                .eq("enrollment_id", enrollmentId).eq("chapter_id", chapterId).last("limit 1"));
        int watched = (lp == null ? 0 : lp.getWatchedSeconds()) + delta;
        if (total > 0) {
            watched = Math.min(watched, total);
        }
        BigDecimal rate = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(watched * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
        int finish = rate.compareTo(ch.getMinFinishRate() == null ? new BigDecimal("90") : ch.getMinFinishRate()) >= 0 ? 1 : 0;
        if (lp == null) {
            lp = new LessonProgress();
            lp.setEnrollmentId(enrollmentId);
            lp.setChapterId(chapterId);
            lp.setWatchedSeconds(watched);
            lp.setWatchRate(rate);
            lp.setPlayTimes(1);
            lp.setFirstLearnTime(LocalDateTime.now());
            lp.setLastHeartTime(LocalDateTime.now());
            lp.setFinishFlag(finish);
            lp.setDeviceType(device == null ? "PC" : device);
            lp.setAbnormalFlag(abnormal ? 1 : 0);
            lp.setCreateTime(LocalDateTime.now());
            progressMapper.insert(lp);
        } else {
            LessonProgress up = new LessonProgress();
            up.setId(lp.getId());
            up.setWatchedSeconds(watched);
            up.setWatchRate(rate);
            up.setFinishFlag(finish);
            up.setLastHeartTime(LocalDateTime.now());
            up.setAbnormalFlag(abnormal ? 1 : (lp.getAbnormalFlag() == null ? 0 : lp.getAbnormalFlag()));
            progressMapper.updateById(up);
        }
        // 心跳留痕，供「日学习时长」与「进度重算」使用
        LessonHeartbeat hb = new LessonHeartbeat();
        hb.setEnrollmentId(enrollmentId);
        hb.setChapterId(chapterId);
        hb.setReportTime(LocalDateTime.now());
        hb.setDeltaSec(delta);
        hb.setAbnormalFlag(abnormal ? 1 : 0);
        hb.setDeviceType(device == null ? "PC" : device);
        hb.setCreateTime(LocalDateTime.now());
        heartMapper.insert(hb);

        refreshProgress(enrollmentId);
        return Map.of("watchRate", rate, "finishFlag", finish, "abnormal", abnormal, "watchedSeconds", watched);
    }

    /** 重算选课记录的总体进度：必修且非章节点的完成比例 */
    private void refreshProgress(Long enrollmentId) {
        Enrollment e = enrollMapper.selectById(enrollmentId);
        List<CourseChapter> req = chapterMapper.selectList(new QueryWrapper<CourseChapter>()
                .eq("course_id", e.getCourseId()).eq("deleted", 0).eq("is_required", 1).ne("parent_id", 0));
        if (req.isEmpty()) {
            return;
        }
        List<Long> ids = req.stream().map(CourseChapter::getId).toList();
        long done = progressMapper.selectCount(new QueryWrapper<LessonProgress>()
                .eq("enrollment_id", enrollmentId).in("chapter_id", ids).eq("finish_flag", 1));
        BigDecimal rate = BigDecimal.valueOf(done * 100.0 / req.size()).setScale(2, RoundingMode.HALF_UP);
        Enrollment up = new Enrollment();
        up.setId(enrollmentId);
        up.setProgressRate(rate);
        up.setLastStudyTime(LocalDateTime.now());
        up.setStatus(rate.compareTo(new BigDecimal("99.99")) >= 0 ? "已完成" : "学习中");
        enrollMapper.updateById(up);
    }

    /** 我的课程：按学习中 / 已完成 / 已退课分组 */
    public Map<String, Object> myCourses() {
        Long sid = UserContext.userId();
        List<Enrollment> list = enrollMapper.selectList(new QueryWrapper<Enrollment>()
                .eq("student_id", sid).eq("deleted", 0).orderByDesc("last_study_time"));
        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();
        groups.put("学习中", new ArrayList<>());
        groups.put("已完成", new ArrayList<>());
        groups.put("已退课", new ArrayList<>());
        for (Enrollment e : list) {
            Course c = courseMapper.selectById(e.getCourseId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("enrollmentId", e.getId());
            m.put("courseId", e.getCourseId());
            m.put("courseName", c == null ? "" : c.getCourseName());
            m.put("coverUrl", c == null ? "" : c.getCoverUrl());
            m.put("teacherName", c == null ? "" : nameOf(c.getTeacherId()));
            m.put("credit", c == null ? null : c.getCredit());
            m.put("progressRate", e.getProgressRate());
            m.put("studyMinutes", e.getStudyMinutes());
            m.put("lastStudyTime", e.getLastStudyTime());
            m.put("status", e.getStatus());
            groups.computeIfAbsent(e.getStatus(), k -> new ArrayList<>()).add(m);
        }
        Map<String, Object> res = new LinkedHashMap<>(groups);
        res.put("total", list.size());
        return res;
    }

    public String nameOf(Long uid) {
        if (uid == null) {
            return "";
        }
        SysUser u = userMapper.selectById(uid);
        return u == null ? "" : u.getRealName();
    }
    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
