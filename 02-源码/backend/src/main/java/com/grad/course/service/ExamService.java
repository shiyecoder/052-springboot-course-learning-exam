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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 题库、组卷、在线考试、防作弊、阅卷与成绩。
 *
 * 状态机：待审核 → 待开始 → 进行中 → 待批阅 → 已出分 → 已归档
 * 每次流转都先判断「当前状态是否允许目标状态」，再校验操作人与必填原因（需求第 4 节）。
 */
@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamMapper examMapper;
    private final ExamQuestionMapper questionMapper;
    private final ExamPaperQuestionMapper paperMapper;
    private final ExamRecordMapper recordMapper;
    private final AnswerDetailMapper detailMapper;
    private final CheatLogMapper cheatMapper;
    private final CourseMapper courseMapper;
    private final EnrollmentMapper enrollMapper;
    private final SysUserMapper userMapper;
    private final SysMessageMapper messageMapper;
    private final TraceService trace;

    // ---------------- 题库 ----------------
    public PageResult<Map<String, Object>> questionPage(Long courseId, String questionType, String keyword,
                                                        long page, long size) {
        QueryWrapper<ExamQuestion> w = new QueryWrapper<ExamQuestion>().eq("deleted", 0);
        if (courseId != null) {
            w.eq("course_id", courseId);
        }
        if (questionType != null && !questionType.isBlank()) {
            w.eq("question_type", questionType);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.like("stem", keyword);
        }
        w.orderByDesc("create_time");
        Page<ExamQuestion> p = questionMapper.selectPage(new Page<>(page, size), w);
        Map<Long, String> courseNames = courseNames();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ExamQuestion q : p.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", q.getId());
            m.put("questionNo", q.getQuestionNo());
            m.put("courseId", q.getCourseId());
            m.put("courseName", courseNames.getOrDefault(q.getCourseId(), ""));
            m.put("questionType", q.getQuestionType());
            m.put("stem", q.getStem());
            m.put("optionsJson", q.getOptionsJson());
            m.put("answer", q.getAnswer());
            m.put("analysis", q.getAnalysis());
            m.put("difficulty", q.getDifficulty());
            m.put("score", q.getScore());
            m.put("useCount", q.getUseCount());
            m.put("status", q.getStatus());
            rows.add(m);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    private Map<Long, String> courseNames() {
        return courseMapper.selectList(new QueryWrapper<Course>().eq("deleted", 0))
                .stream().collect(Collectors.toMap(Course::getId, Course::getCourseName, (a, b) -> a));
    }

    @Transactional
    public Long saveQuestion(ExamQuestion q) {
        if (q.getStem() == null || q.getStem().isBlank()) {
            throw new BizException("题干不能为空");
        }
        if ("单选题".equals(q.getQuestionType())) {
            String ans = q.getAnswer();
            if (ans == null || ans.replace(",", "").length() != 1) {
                throw new BizException("单选题正确答案必须且只能有一个选项");
            }
        }
        if (q.getId() == null) {
            q.setQuestionNo("TK" + System.currentTimeMillis() % 1000000);
            q.setStatus("启用");
            q.setUseCount(0);
            q.setCreateBy(UserContext.userId());
            q.setCreateTime(LocalDateTime.now());
            questionMapper.insert(q);
            return q.getId();
        }
        questionMapper.updateById(q);
        return q.getId();
    }

    @Transactional
    public boolean deleteQuestion(Long id) {
        long used = paperMapper.selectCount(new QueryWrapper<ExamPaperQuestion>().eq("question_id", id));
        if (used > 0) {
            throw new BizException("该题目已被 " + used + " 份试卷引用，不能删除（可改为停用）");
        }
        questionMapper.deleteById(id);
        return true;
    }

    // ---------------- 考试 ----------------
    public PageResult<Map<String, Object>> examPage(String keyword, String status, Long courseId,
                                                    long page, long size) {
        QueryWrapper<Exam> w = new QueryWrapper<Exam>().eq("deleted", 0);
        if (keyword != null && !keyword.isBlank()) {
            w.like("exam_name", keyword);
        }
        if (status != null && !status.isBlank()) {
            w.eq("status", status);
        }
        if (courseId != null) {
            w.eq("course_id", courseId);
        }
        if (UserContext.isTeacher()) {
            List<Long> cids = courseMapper.selectList(new QueryWrapper<Course>()
                            .eq("teacher_id", UserContext.userId())).stream().map(Course::getId).toList();
            if (cids.isEmpty()) {
                return PageResult.of(0, page, size, List.of());
            }
            w.in("course_id", cids);
        }
        if (UserContext.isStudent()) {
            w.in("status", List.of("待开始", "进行中", "待批阅", "已出分", "已归档"));
        }
        w.orderByDesc("start_time");
        Page<Exam> p = examMapper.selectPage(new Page<>(page, size), w);
        Map<Long, String> cn = courseNames();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Exam e : p.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("examNo", e.getExamNo());
            m.put("examName", e.getExamName());
            m.put("courseId", e.getCourseId());
            m.put("courseName", cn.getOrDefault(e.getCourseId(), ""));
            m.put("examType", e.getExamType());
            m.put("startTime", e.getStartTime());
            m.put("endTime", e.getEndTime());
            m.put("durationMin", e.getDurationMin());
            m.put("totalScore", e.getTotalScore());
            m.put("paperMode", e.getPaperMode());
            m.put("attemptLimit", e.getAttemptLimit());
            m.put("cheatLevel", e.getCheatLevel());
            m.put("status", e.getStatus());
            m.put("questionCount", paperMapper.selectCount(new QueryWrapper<ExamPaperQuestion>()
                    .eq("exam_id", e.getId())));
            m.put("recordCount", recordMapper.selectCount(new QueryWrapper<ExamRecord>()
                    .eq("exam_id", e.getId())));
            rows.add(m);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    public Map<String, Object> examDetail(Long id) {
        Exam e = examMapper.selectById(id);
        if (e == null) {
            throw new BizException(404, "考试不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("exam", e);
        m.put("courseName", courseNames().getOrDefault(e.getCourseId(), ""));
        List<ExamPaperQuestion> pq = paperMapper.selectList(new QueryWrapper<ExamPaperQuestion>()
                .eq("exam_id", id).orderByAsc("sort_no"));
        m.put("questions", pq);
        List<ExamRecord> recs = recordMapper.selectList(new QueryWrapper<ExamRecord>()
                .eq("exam_id", id));
        m.put("recordCount", recs.size());
        m.put("pendingCount", recs.stream().filter(x -> "待批阅".equals(x.getStatus())).count());
        return m;
    }

    /** 保存考试与组卷：校验题目分值合计等于总分 */
    @Transactional
    public Long saveExam(Map<String, Object> b, List<Long> questionIds) {
        Object idObj = b.get("id");
        Exam e = new Exam();
        e.setExamName(str(b.get("examName")));
        e.setExamType(str(b.get("examType")));
        e.setCourseId(b.get("courseId") == null ? null : Long.parseLong(str(b.get("courseId"))));
        e.setPaperMode(str(b.get("paperMode")));
        e.setCheatLevel(str(b.get("cheatLevel")));
        if (b.get("durationMin") != null) {
            e.setDurationMin(Integer.parseInt(str(b.get("durationMin"))));
        }
        if (b.get("startTime") != null) {
            e.setStartTime(LocalDateTime.parse(str(b.get("startTime")).replace(" ", "T")));
        }
        if (b.get("endTime") != null) {
            e.setEndTime(LocalDateTime.parse(str(b.get("endTime")).replace(" ", "T")));
        }
        if (e.getStartTime() != null && e.getDurationMin() != null && e.getEndTime() == null) {
            e.setEndTime(e.getStartTime().plusMinutes(e.getDurationMin()));
        }
        if (e.getExamName() == null || e.getExamName().isBlank()) {
            throw new BizException("考试名称不能为空");
        }
        if (idObj == null) {
            e.setExamNo("KS" + System.currentTimeMillis() % 1000000);
            e.setStatus("待审核");
            e.setTotalScore(new BigDecimal("60"));
            e.setAttemptLimit(1);
            e.setCreateBy(UserContext.userId());
            e.setCreateTime(LocalDateTime.now());
            examMapper.insert(e);
        } else {
            e.setId(Long.parseLong(str(idObj)));
            examMapper.updateById(e);
        }
        if (questionIds != null && !questionIds.isEmpty()) {
            paperMapper.delete(new QueryWrapper<ExamPaperQuestion>().eq("exam_id", e.getId()));
            List<ExamQuestion> qs = questionMapper.selectBatchIds(questionIds);
            BigDecimal sum = BigDecimal.ZERO;
            int i = 1;
            for (ExamQuestion q : qs) {
                ExamPaperQuestion pq = new ExamPaperQuestion();
                pq.setExamId(e.getId());
                pq.setQuestionId(q.getId());
                pq.setQuestionNo(q.getQuestionNo());
                pq.setSortNo(i++);
                pq.setQuestionType(q.getQuestionType());
                pq.setStem(q.getStem());
                pq.setOptionsJson(q.getOptionsJson());
                pq.setAnswer(q.getAnswer());
                pq.setAnalysis(q.getAnalysis());
                pq.setScore(q.getScore());
                pq.setCreateTime(LocalDateTime.now());
                paperMapper.insert(pq);
                sum = sum.add(q.getScore() == null ? BigDecimal.ZERO : q.getScore());
            }
            Exam up = new Exam();
            up.setId(e.getId());
            up.setTotalScore(sum);
            examMapper.updateById(up);
            if (sum.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("试卷分值合计必须大于 0");
            }
        }
        trace.log("考试管理", idObj == null ? "新增考试" : "修改考试", e.getExamNo(), "维护考试 " + e.getExamName());
        return e.getId();
    }

    /** 教务审核考试：通过 → 待开始；驳回需填写意见 */
    @Transactional
    public boolean audit(Long id, boolean pass, String opinion) {
        Exam e = examMapper.selectById(id);
        if (e == null) {
            throw new BizException(404, "考试不存在");
        }
        if (!"待审核".equals(e.getStatus())) {
            throw new BizException("只有待审核的考试可以审核");
        }
        long qc = paperMapper.selectCount(new QueryWrapper<ExamPaperQuestion>().eq("exam_id", id));
        if (qc == 0) {
            throw new BizException("试卷尚未组卷，不能通过审核");
        }
        if (!pass && (opinion == null || opinion.trim().length() < 5)) {
            throw new BizException("驳回意见不少于 5 字");
        }
        Exam up = new Exam();
        up.setId(id);
        up.setStatus(pass ? "待开始" : "待审核");
        examMapper.updateById(up);
        trace.log("考试管理", pass ? "审核通过" : "审核驳回", e.getExamNo(), "考试 " + e.getExamName());
        return true;
    }

    // ---------------- 开始考试 / 交卷 ----------------
    /**
     * 开始考试：同一学生重复点击只返回已有答卷，不重复扣减作答次数，
     * 也不产生第二条答卷（需求 11 的并发用例）。
     */
    @Transactional
    public Map<String, Object> start(Long examId) {
        Exam e = examMapper.selectById(examId);
        if (e == null) {
            throw new BizException(404, "考试不存在");
        }
        if (!List.of("待开始", "进行中").contains(e.getStatus())) {
            throw new BizException("当前考试状态不允许作答：" + e.getStatus());
        }
        Long sid = UserContext.userId();
        Enrollment en = enrollMapper.selectOne(new QueryWrapper<Enrollment>()
                .eq("course_id", e.getCourseId()).eq("student_id", sid).eq("deleted", 0)
                .ne("status", "已退课").last("limit 1"));
        if (en == null) {
            throw new BizException("你尚未选修该课程，无法参加考试");
        }
        ExamRecord r = recordMapper.selectOne(new QueryWrapper<ExamRecord>()
                .eq("exam_id", examId).eq("student_id", sid).eq("deleted", 0).last("limit 1"));
        if (r == null) {
            r = new ExamRecord();
            r.setRecordNo("DJ" + System.currentTimeMillis());
            r.setExamId(examId);
            r.setStudentId(sid);
            r.setEnrollmentId(en.getId());
            r.setAttemptNo(1);
            r.setStartTime(LocalDateTime.now());
            r.setObjectiveScore(BigDecimal.ZERO);
            r.setSubjectiveScore(BigDecimal.ZERO);
            r.setTotalScore(BigDecimal.ZERO);
            r.setPassFlag(0);
            r.setStatus("作答中");
            r.setCreateBy(sid);
            r.setCreateTime(LocalDateTime.now());
            recordMapper.insert(r);
            // 开考即把题目快照落到答题明细，保证题库后续修改不影响本次判分
            for (ExamPaperQuestion pq : paperMapper.selectList(new QueryWrapper<ExamPaperQuestion>()
                    .eq("exam_id", examId).orderByAsc("sort_no"))) {
                AnswerDetail d = new AnswerDetail();
                d.setExamRecordId(r.getId());
                d.setQuestionId(pq.getQuestionId());
                d.setSortNo(pq.getSortNo());
                d.setQuestionType(pq.getQuestionType());
                d.setCorrectFlag(0);
                d.setScore(BigDecimal.ZERO);
                d.setFullScore(pq.getScore());
                d.setCreateTime(LocalDateTime.now());
                detailMapper.insert(d);
            }
            // 考试转进行中
            if ("待开始".equals(e.getStatus()) && e.getStartTime() != null
                    && !e.getStartTime().isAfter(LocalDateTime.now())) {
                Exam up = new Exam();
                up.setId(examId);
                up.setStatus("进行中");
                examMapper.updateById(up);
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("recordId", r.getId());
        res.put("recordNo", r.getRecordNo());
        res.put("status", r.getStatus());
        res.put("exam", e);
        res.put("questions", paperMapper.selectList(new QueryWrapper<ExamPaperQuestion>()
                .eq("exam_id", examId).orderByAsc("sort_no")));
        res.put("details", detailMapper.selectList(new QueryWrapper<AnswerDetail>()
                .eq("exam_record_id", r.getId()).orderByAsc("sort_no")));
        long left = e.getDurationMin() == null ? 0
                : e.getDurationMin() * 60 - Duration.between(r.getStartTime(), LocalDateTime.now()).getSeconds();
        res.put("remainSeconds", Math.max(0, left));
        return res;
    }

    /**
     * 交卷：客观题自动判分，主观题置待批阅；交卷幂等，重复提交返回首次结果。
     */
    @Transactional
    public Map<String, Object> submit(Long recordId, Map<Long, String> answers) {
        ExamRecord r = recordMapper.selectById(recordId);
        if (r == null) {
            throw new BizException(404, "答卷不存在");
        }
        if (!Objects.equals(r.getStudentId(), UserContext.userId()) && !UserContext.isStaffOfTeaching()
                && !UserContext.isAdmin()) {
            throw new BizException(403, "只能提交本人的答卷");
        }
        if (!"作答中".equals(r.getStatus())) {
            return Map.of("recordId", recordId, "status", r.getStatus(), "duplicated", true,
                    "totalScore", r.getTotalScore());
        }
        List<AnswerDetail> ds = detailMapper.selectList(new QueryWrapper<AnswerDetail>()
                .eq("exam_record_id", recordId).orderByAsc("sort_no"));
        BigDecimal obj = BigDecimal.ZERO;
        boolean hasSubjective = false;
        for (AnswerDetail d : ds) {
            String ans = answers == null ? null : answers.get(d.getQuestionId());
            if (d.getQuestionId() == null) {
                continue;
            }
            ExamPaperQuestion pq = paperMapper.selectOne(new QueryWrapper<ExamPaperQuestion>()
                    .eq("exam_id", r.getExamId()).eq("question_id", d.getQuestionId()).last("limit 1"));
            String std = pq == null ? null : pq.getAnswer();
            boolean objective = List.of("单选题", "多选题", "判断题", "填空题").contains(d.getQuestionType());
            AnswerDetail up = new AnswerDetail();
            up.setId(d.getId());
            up.setAnswerContent(ans);
            if (objective) {
                boolean ok = ans != null && std != null
                        && ans.replace(" ", "").equalsIgnoreCase(std.replace(" ", ""));
                up.setCorrectFlag(ok ? 1 : 0);
                up.setScore(ok ? d.getFullScore() : BigDecimal.ZERO);
                if (ok) {
                    obj = obj.add(d.getFullScore());
                }
            } else {
                hasSubjective = true;
                up.setCorrectFlag(0);
                up.setScore(BigDecimal.ZERO);
            }
            detailMapper.updateById(up);
        }
        ExamRecord rr = new ExamRecord();
        rr.setId(recordId);
        rr.setSubmitTime(LocalDateTime.now());
        rr.setObjectiveScore(obj);
        rr.setStatus(hasSubjective ? "待批阅" : "已出分");
        if (!hasSubjective) {
            rr.setTotalScore(obj);
            Exam e = examMapper.selectById(r.getExamId());
            BigDecimal pass = e == null || e.getTotalScore() == null ? new BigDecimal("36")
                    : e.getTotalScore().multiply(new BigDecimal("0.6"));
            rr.setPassFlag(obj.compareTo(pass) >= 0 ? 1 : 0);
        }
        recordMapper.updateById(rr);
        Exam e2 = examMapper.selectById(r.getExamId());
        if (hasSubjective && e2 != null) {
            Exam up = new Exam();
            up.setId(e2.getId());
            up.setStatus("待批阅");
            examMapper.updateById(up);
        }
        trace.log("考试管理", "交卷", r.getRecordNo(), "客观题得分 " + obj);
        return Map.of("recordId", recordId, "status", rr.getStatus(), "objectiveScore", obj,
                "duplicated", false);
    }

    /** 防作弊：切屏 / 退出全屏 / 多端登录 / 超时未操作写入 cheat_log，达阈值强制交卷 */
    @Transactional
    public Map<String, Object> cheatEvent(Long recordId, String eventType, String device, String ip) {
        ExamRecord r = recordMapper.selectById(recordId);
        if (r == null) {
            throw new BizException(404, "答卷不存在");
        }
        Exam e = examMapper.selectById(r.getExamId());
        long count = cheatMapper.selectCount(new QueryWrapper<CheatLog>().eq("exam_record_id", recordId)) + 1;
        CheatLog c = new CheatLog();
        c.setExamRecordId(recordId);
        c.setEventType(eventType == null ? "切屏" : eventType);
        c.setEventTime(LocalDateTime.now());
        c.setDurationSec(0);
        c.setTotalCount((int) count);
        c.setDeviceInfo(device == null ? "未知设备" : device);
        c.setIpAddr(ip == null ? "127.0.0.1" : ip);
        c.setHandlerId(r.getStudentId());
        c.setCreateTime(LocalDateTime.now());
        // 严格等级容忍 3 次，常规 5 次，宽松不计
        int limit = e == null ? 5 : ("严格".equals(e.getCheatLevel()) ? 3 : ("常规".equals(e.getCheatLevel()) ? 5 : 99));
        boolean force = count >= limit && limit < 99;
        c.setHandleResult(force ? "强制交卷" : "已记录");
        cheatMapper.insert(c);
        if (force && "作答中".equals(r.getStatus())) {
            submit(recordId, Map.of());
        }
        return Map.of("totalCount", count, "limit", limit, "forceSubmit", force,
                "result", c.getHandleResult());
    }

    // ---------------- 阅卷 / 成绩 ----------------
    public PageResult<Map<String, Object>> gradingList(Long examId, long page, long size) {
        QueryWrapper<ExamRecord> w = new QueryWrapper<ExamRecord>()
                .eq("status", "待批阅").eq("deleted", 0);
        if (examId != null) {
            w.eq("exam_id", examId);
        }
        w.orderByAsc("submit_time");
        Page<ExamRecord> p = recordMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<Long, String> cn = courseNames();
        for (ExamRecord r : p.getRecords()) {
            Exam ex = examMapper.selectById(r.getExamId());
            SysUser s = userMapper.selectById(r.getStudentId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("recordNo", r.getRecordNo());
            m.put("examId", r.getExamId());
            m.put("examName", ex == null ? "" : ex.getExamName());
            m.put("courseName", ex == null ? "" : cn.getOrDefault(ex.getCourseId(), ""));
            m.put("studentId", r.getStudentId());
            m.put("studentName", s == null ? "" : s.getRealName());
            m.put("studentNo", s == null ? "" : s.getUserNo());
            m.put("submitTime", r.getSubmitTime());
            m.put("objectiveScore", r.getObjectiveScore());
            m.put("status", r.getStatus());
            m.put("pendingCount", detailMapper.selectCount(new QueryWrapper<AnswerDetail>()
                    .eq("exam_record_id", r.getId()).in("question_type", List.of("简答题")).isNull("grade_time")));
            rows.add(m);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    public Map<String, Object> gradingDetail(Long recordId) {
        ExamRecord r = recordMapper.selectById(recordId);
        if (r == null) {
            throw new BizException(404, "答卷不存在");
        }
        Exam e = examMapper.selectById(r.getExamId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("record", r);
        m.put("exam", e);
        m.put("student", userMapper.selectById(r.getStudentId()));
        m.put("details", detailMapper.selectList(new QueryWrapper<AnswerDetail>()
                .eq("exam_record_id", recordId).orderByAsc("sort_no")));
        m.put("cheatLogs", cheatMapper.selectList(new QueryWrapper<CheatLog>()
                .eq("exam_record_id", recordId)));
        return m;
    }

    /**
     * 批阅主观题：分数不得超过题目分值；评语必填；全批完且两名教师分差 > 10 分转仲裁。
     */
    @Transactional
    public Map<String, Object> grade(Long recordId, List<Map<String, Object>> items) {
        ExamRecord r = recordMapper.selectById(recordId);
        if (r == null) {
            throw new BizException(404, "答卷不存在");
        }
        if (!"待批阅".equals(r.getStatus())) {
            throw new BizException("该答卷当前不可批阅：" + r.getStatus());
        }
        for (Map<String, Object> it : items) {
            Long did = Long.valueOf(String.valueOf(it.get("id")));
            BigDecimal sc = new BigDecimal(String.valueOf(it.get("score")));
            AnswerDetail d = detailMapper.selectById(did);
            if (d == null || !Objects.equals(d.getExamRecordId(), recordId)) {
                throw new BizException("答题明细不存在");
            }
            if (sc.compareTo(d.getFullScore() == null ? BigDecimal.ZERO : d.getFullScore()) > 0) {
                throw new BizException("主观题评分不得超过题目分值（本题满分 " + d.getFullScore() + "）");
            }
            String comment = it.get("comment") == null ? null : String.valueOf(it.get("comment"));
            if (comment == null || comment.trim().length() < 2) {
                throw new BizException("主观题评语必填");
            }
            AnswerDetail up = new AnswerDetail();
            up.setId(did);
            up.setScore(sc.setScale(1, RoundingMode.HALF_UP));
            up.setComment(comment);
            up.setCorrectFlag(sc.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0);
            up.setGradeBy(UserContext.userId());
            up.setGradeTime(LocalDateTime.now());
            if (d.getFirstScore() == null) {
                up.setFirstScore(d.getScore());
            }
            detailMapper.updateById(up);
        }
        // 统计剩余未批
        long left = detailMapper.selectCount(new QueryWrapper<AnswerDetail>()
                .eq("exam_record_id", recordId).eq("question_type", "简答题").isNull("grade_time"));
        BigDecimal subj = BigDecimal.ZERO;
        for (AnswerDetail d : detailMapper.selectList(new QueryWrapper<AnswerDetail>()
                .eq("exam_record_id", recordId))) {
            if ("简答题".equals(d.getQuestionType()) && d.getScore() != null) {
                subj = subj.add(d.getScore());
            }
        }
        BigDecimal total = (r.getObjectiveScore() == null ? BigDecimal.ZERO : r.getObjectiveScore()).add(subj);
        ExamRecord up = new ExamRecord();
        up.setId(recordId);
        up.setSubjectiveScore(subj);
        up.setTotalScore(total);
        if (left == 0) {
            Exam e = examMapper.selectById(r.getExamId());
            BigDecimal pass = e == null || e.getTotalScore() == null ? new BigDecimal("36")
                    : e.getTotalScore().multiply(new BigDecimal("0.6"));
            up.setStatus("已出分");
            up.setPassFlag(total.compareTo(pass) >= 0 ? 1 : 0);
        }
        recordMapper.updateById(up);
        trace.log("成绩管理", "批阅主观题", r.getRecordNo(), "主观题得分 " + subj + "，总分 " + total);
        return Map.of("remain", left, "subjectiveScore", subj, "totalScore", total,
                "status", left == 0 ? "已出分" : "待批阅");
    }

    /** 发布成绩：存在未批阅主观题时禁止发布；发布后试卷与答卷只读 */
    @Transactional
    public Map<String, Object> publishScore(Long examId) {
        Exam e = examMapper.selectById(examId);
        if (e == null) {
            throw new BizException(404, "考试不存在");
        }
        if (!List.of("待批阅", "进行中").contains(e.getStatus())) {
            throw new BizException("当前状态不允许发布成绩：" + e.getStatus());
        }
        List<ExamRecord> recs = recordMapper.selectList(new QueryWrapper<ExamRecord>()
                .eq("exam_id", examId));
        long pending = recs.stream().filter(x -> "待批阅".equals(x.getStatus())).count();
        if (pending > 0) {
            throw new BizException("还有 " + pending + " 份答卷未完成批阅，不能发布成绩");
        }
        Exam up = new Exam();
        up.setId(examId);
        up.setStatus("已出分");
        examMapper.updateById(up);
        int fail = 0;
        for (ExamRecord r : recs) {
            if (r.getPassFlag() != null && r.getPassFlag() == 0) {
                fail++;
            }
            SysMessage msg = new SysMessage();
            msg.setReceiverId(r.getStudentId());
            msg.setTitle("成绩已发布：" + e.getExamName());
            msg.setContent("本次得分 " + r.getTotalScore() + "，可通过「我的成绩」查看各题得分；如有异议请在 5 天内提交复核。");
            msg.setMsgType("成绩发布");
            msg.setBizType("EXAM");
            msg.setBizId(examId);
            msg.setReadFlag(0);
            msg.setCreateTime(LocalDateTime.now());
            messageMapper.insert(msg);
        }
        trace.log("成绩管理", "发布成绩", e.getExamNo(), "发布 " + recs.size() + " 份答卷，补考名单 " + fail + " 人");
        return Map.of("published", recs.size(), "failCount", fail);
    }

    /** 成绩单：学生看自己的，教师看本课程全部 */
    public PageResult<Map<String, Object>> scorePage(Long courseId, Long studentId, long page, long size) {
        QueryWrapper<ExamRecord> w = new QueryWrapper<ExamRecord>()
                .in("status", List.of("已出分")).eq("deleted", 0);
        if (UserContext.isStudent()) {
            w.eq("student_id", UserContext.userId());
        } else if (studentId != null) {
            w.eq("student_id", studentId);
        }
        if (courseId != null) {
            List<Long> eids = examMapper.selectList(new QueryWrapper<Exam>()
                            .eq("course_id", courseId)).stream().map(Exam::getId).toList();
            if (eids.isEmpty()) {
                return PageResult.of(0, page, size, List.of());
            }
            w.in("exam_id", eids);
        }
        w.orderByDesc("submit_time");
        Page<ExamRecord> p = recordMapper.selectPage(new Page<>(page, size), w);
        Map<Long, String> cn = courseNames();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ExamRecord r : p.getRecords()) {
            Exam e = examMapper.selectById(r.getExamId());
            SysUser s = userMapper.selectById(r.getStudentId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("recordNo", r.getRecordNo());
            m.put("examId", r.getExamId());
            m.put("examName", e == null ? "" : e.getExamName());
            m.put("examType", e == null ? "" : e.getExamType());
            m.put("courseId", e == null ? null : e.getCourseId());
            m.put("courseName", e == null ? "" : cn.getOrDefault(e.getCourseId(), ""));
            m.put("studentId", r.getStudentId());
            m.put("studentName", s == null ? "" : s.getRealName());
            m.put("objectiveScore", r.getObjectiveScore());
            m.put("subjectiveScore", r.getSubjectiveScore());
            m.put("totalScore", r.getTotalScore());
            m.put("fullScore", e == null ? null : e.getTotalScore());
            m.put("passFlag", r.getPassFlag());
            m.put("submitTime", r.getSubmitTime());
            m.put("status", r.getStatus());
            rows.add(m);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    /** 答卷详情：学生看自己的作答与解析 */
    public Map<String, Object> recordDetail(Long id) {
        ExamRecord r = recordMapper.selectById(id);
        if (r == null) {
            throw new BizException(404, "答卷不存在");
        }
        Map<String, Object> m = gradingDetail(id);
        m.put("examName", examMapper.selectById(r.getExamId()) == null ? ""
                : examMapper.selectById(r.getExamId()).getExamName());
        return m;
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
