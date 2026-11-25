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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 创新点：学习进度与挂科风险预警。
 *
 * 风险分 = 100 − Σ(单项归一化得分 × 权重)，权重合计 100：
 * 视频观看完成率 30%、提交及时率 20%、测验得分率 25%、近 14 天登录频次 15%、讨论参与度 10%。
 *
 * 每一次计算都保存输入快照 + 规则版本 + 各因子得分 + 总分 + 等级（risk_snapshot 不覆盖历史），
 * 人工修正单独写 risk_correction，原始结果保留，供「推荐采纳率 / 人工修正率」统计。
 */
@Service
@RequiredArgsConstructor
public class RiskService {

    private final RiskRuleMapper ruleMapper;
    private final RiskSnapshotMapper snapMapper;
    private final RiskCorrectionMapper corrMapper;
    private final EnrollmentMapper enrollMapper;
    private final CourseMapper courseMapper;
    private final CourseChapterMapper chapterMapper;
    private final LessonProgressMapper progressMapper;
    private final LessonHeartbeatMapper heartMapper;
    private final ExamRecordMapper recordMapper;
    private final ExamMapper examMapper;
    private final AnswerDetailMapper detailMapper;
    private final DiscussionPostMapper postMapper;
    private final DiscussionReplyMapper replyMapper;
    private final SysUserMapper userMapper;
    private final TraceService trace;

    public List<RiskRule> rules() {
        return ruleMapper.selectList(new QueryWrapper<RiskRule>().eq("deleted", 0).orderByDesc("effect_time"));
    }

    public RiskRule currentRule() {
        RiskRule r = ruleMapper.selectOne(new QueryWrapper<RiskRule>().eq("deleted", 0)
                .eq("status", "启用").orderByDesc("effect_time").last("limit 1"));
        if (r == null) {
            r = ruleMapper.selectOne(new QueryWrapper<RiskRule>().eq("deleted", 0)
                    .orderByDesc("effect_time").last("limit 1"));
        }
        if (r == null) {
            throw new BizException("尚未配置预警规则");
        }
        return r;
    }

    /** 保存规则：权重合计必须等于 100%，新版本号生效，旧版本置为历史 */
    @Transactional
    public Long saveRule(RiskRule r) {
        BigDecimal sum = nz(r.getWeightWatch()).add(nz(r.getWeightSubmit())).add(nz(r.getWeightQuiz()))
                .add(nz(r.getWeightLogin())).add(nz(r.getWeightDiscuss()));
        if (sum.compareTo(new BigDecimal("100.00")) != 0) {
            throw new BizException("五项权重合计必须等于 100%，当前为 " + sum + "%");
        }
        if (r.getLowLine() == null || r.getFocusLine() == null || r.getMidLine() == null) {
            throw new BizException("请完整填写分级阈值");
        }
        if (r.getId() == null) {
            r.setRuleVersion("V" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmm")));
            r.setEffectTime(LocalDateTime.now());
            r.setStatus("启用");
            r.setCreateBy(UserContext.userId());
            r.setCreateTime(LocalDateTime.now());
            ruleMapper.insert(r);
        } else {
            RiskRule old = ruleMapper.selectById(r.getId());
            if (old == null) {
                throw new BizException(404, "规则不存在");
            }
            ruleMapper.updateById(r);
        }
        // 其他规则转历史，保证同一时刻只有一条启用
        ruleMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<RiskRule>()
                .set("status", "历史").ne("id", r.getId()).eq("status", "启用"));
        trace.log("学习预警", "保存规则", r.getRuleVersion(),
                "权重 " + sum + "%，分级 " + r.getLowLine() + "/" + r.getFocusLine() + "/" + r.getMidLine());
        return r.getId();
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** 计算单个选课记录的风险分并落快照，返回完整解释（因子、短板、建议、缺失数据） */
    @Transactional
    public Map<String, Object> evaluate(Long enrollmentId) {
        Enrollment e = enrollMapper.selectById(enrollmentId);
        if (e == null) {
            throw new BizException(404, "选课记录不存在");
        }
        RiskRule rule = currentRule();
        Long cid = e.getCourseId();
        Long sid = e.getStudentId();

        // 因子 1：视频观看完成率 —— 必修节点完成比例（缺失按 0 分并标注）
        List<CourseChapter> req = chapterMapper.selectList(new QueryWrapper<CourseChapter>()
                .eq("course_id", cid).eq("deleted", 0).eq("is_required", 1).ne("parent_id", 0));
        List<Long> reqIds = req.stream().map(CourseChapter::getId).toList();
        long doneNodes = reqIds.isEmpty() ? 0 : progressMapper.selectCount(new QueryWrapper<LessonProgress>()
                .eq("enrollment_id", enrollmentId).in("chapter_id", reqIds).eq("finish_flag", 1));
        double watchRate = reqIds.isEmpty() ? 0 : doneNodes * 100.0 / reqIds.size();
        boolean noStudy = progressMapper.selectCount(new QueryWrapper<LessonProgress>()
                .eq("enrollment_id", enrollmentId)) == 0;

        // 因子 2：提交及时率 —— 已提交答卷 / 应参加考试数（应提交为 0 时给 60 基准分）
        List<Long> examIds = examMapper.selectList(new QueryWrapper<Exam>().eq("course_id", cid)
                        .eq("deleted", 0).in("status", List.of("进行中", "待批阅", "已出分", "已归档")))
                .stream().map(Exam::getId).toList();
        long submitted = examIds.isEmpty() ? 0 : recordMapper.selectCount(new QueryWrapper<ExamRecord>()
                .eq("student_id", sid).in("exam_id", examIds).ne("status", "作答中"));
        double submitRate = examIds.isEmpty() ? 60 : Math.min(100, submitted * 100.0 / examIds.size());

        // 因子 3：阶段测验得分率 —— 已出分答卷的平均得分率
        List<ExamRecord> recs = examIds.isEmpty() ? List.of() : recordMapper.selectList(
                new QueryWrapper<ExamRecord>().eq("student_id", sid).in("exam_id", examIds)
                        .eq("status", "已出分"));
        double quizRate = 0;
        if (!recs.isEmpty()) {
            double sum = 0;
            int n = 0;
            for (ExamRecord r : recs) {
                Exam ex = examMapper.selectById(r.getExamId());
                BigDecimal full = ex == null || ex.getTotalScore() == null ? BigDecimal.ZERO : ex.getTotalScore();
                if (full.compareTo(BigDecimal.ZERO) > 0 && r.getTotalScore() != null) {
                    sum += Math.min(100, r.getTotalScore().doubleValue() * 100.0 / full.doubleValue());
                    n++;
                }
            }
            quizRate = n == 0 ? 0 : sum / n;
        }
        boolean noQuiz = recs.isEmpty();

        // 因子 4：近 14 天登录频次 —— 心跳记录按自然日去重，0 天 0 分、1~2 天 40 分、3~5 天 70 分、6 天以上 100 分
        LocalDateTime since = LocalDateTime.now().minusDays(14);
        Set<LocalDate> days = progressMapper.selectList(new QueryWrapper<LessonProgress>()
                        .eq("enrollment_id", enrollmentId).ge("last_heart_time", since))
                .stream().map(p -> p.getLastHeartTime().toLocalDate()).collect(Collectors.toSet());
        int loginDays = days.size();
        double loginScore = loginDays == 0 ? 0 : (loginDays <= 2 ? 40 : (loginDays <= 5 ? 70 : 100));

        // 因子 5：讨论参与度 —— 有效发帖 + 回帖 + 被采纳，自问自答不计入
        List<DiscussionPost> posts = postMapper.selectList(new QueryWrapper<DiscussionPost>()
                .eq("course_id", cid).eq("author_id", sid).eq("deleted", 0));
        List<DiscussionReply> replies = replyMapper.selectList(new QueryWrapper<DiscussionReply>()
                .eq("author_id", sid).eq("deleted", 0));
        long adopted = replies.stream().filter(x -> x.getIsAdopted() != null && x.getIsAdopted() == 1).count();
        long effective = posts.size() + replies.size() - adopted;
        double discussScore = Math.min(100, effective * 12 + adopted * 10);

        double sWatch = Math.min(100, watchRate);
        double sSubmit = Math.min(100, submitRate);
        double sQuiz = Math.min(100, quizRate);
        double sLogin = loginScore;
        double sDiscuss = discussScore;

        BigDecimal w1 = nz(rule.getWeightWatch()).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal w2 = nz(rule.getWeightSubmit()).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal w3 = nz(rule.getWeightQuiz()).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal w4 = nz(rule.getWeightLogin()).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal w5 = nz(rule.getWeightDiscuss()).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

        double risk = 100 - (sWatch * w1.doubleValue() + sSubmit * w2.doubleValue() + sQuiz * w3.doubleValue()
                + sLogin * w4.doubleValue() + sDiscuss * w5.doubleValue());
        risk = Math.max(0, Math.min(100, risk));
        String level = risk >= rule.getLowLine().doubleValue() ? "低风险"
                : (risk >= rule.getFocusLine().doubleValue() ? "关注"
                : (risk >= rule.getMidLine().doubleValue() ? "中风险" : "高风险"));

        // 主要短板：权重 × 失分 最大的因子
        double[] losses = {(100 - sWatch) * w1.doubleValue(), (100 - sSubmit) * w2.doubleValue(),
                (100 - sQuiz) * w3.doubleValue(), (100 - sLogin) * w4.doubleValue(),
                (100 - sDiscuss) * w5.doubleValue()};
        String[] names = {"视频观看完成率", "作业与测验提交及时率", "阶段测验得分率", "近 14 天登录学习频次", "讨论区与答疑参与度"};
        int maxIdx = 0;
        for (int i = 1; i < losses.length; i++) {
            if (losses[i] > losses[maxIdx]) {
                maxIdx = i;
            }
        }
        List<String> missing = new ArrayList<>();
        if (noStudy) {
            missing.add("无学习行为");
        }
        if (noQuiz) {
            missing.add("缺少测评数据");
        }
        if (examIds.isEmpty()) {
            missing.add("该课程尚未安排测评");
        }
        String gap = "主要短板：" + names[maxIdx] + "（失分 " + String.format("%.1f", losses[maxIdx]) + " 分）";
        String suggestion = switch (level) {
            case "低风险" -> "学习节奏正常，仅推送章节更新提醒";
            case "关注" -> "向学生推送学习计划，暂不通知辅导员";
            case "中风险" -> "生成预警单并抄送任课教师，要求 7 天内完成补学";
            default -> "抄送辅导员与院系，限制参加期末考试直至完成补学与补测";
        };

        RiskSnapshot s = new RiskSnapshot();
        s.setSnapshotNo("YJ" + System.currentTimeMillis() % 10000000);
        s.setStudentId(sid);
        s.setCourseId(cid);
        s.setEnrollmentId(enrollmentId);
        s.setRuleId(rule.getId());
        s.setRuleVersion(rule.getRuleVersion());
        s.setRawWatch(bd(watchRate));
        s.setRawSubmit(bd(submitRate));
        s.setRawQuiz(bd(quizRate));
        s.setRawLogin(bd(loginDays * 20.0));
        s.setRawDiscuss(bd(effective));
        s.setScoreWatch(bd(sWatch));
        s.setScoreSubmit(bd(sSubmit));
        s.setScoreQuiz(bd(sQuiz));
        s.setScoreLogin(bd(sLogin));
        s.setScoreDiscuss(bd(sDiscuss));
        s.setRiskScore(bd(risk));
        s.setRiskLevel(level);
        s.setGapText(gap);
        s.setSuggestion(suggestion);
        s.setMissingFlag(missing.isEmpty() ? null : String.join(",", missing));
        s.setCalcTime(LocalDateTime.now());
        s.setCalcBy(UserContext.userId());
        s.setAdoptFlag(0);
        s.setCreateTime(LocalDateTime.now());
        snapMapper.insert(s);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("snapshotId", s.getId());
        res.put("snapshotNo", s.getSnapshotNo());
        res.put("riskScore", s.getRiskScore());
        res.put("riskLevel", level);
        res.put("ruleVersion", rule.getRuleVersion());
        res.put("gapText", gap);
        res.put("suggestion", suggestion);
        res.put("missing", missing);
        res.put("studentName", nameOf(sid));
        Course c = courseMapper.selectById(cid);
        res.put("courseName", c == null ? "" : c.getCourseName());
        List<Map<String, Object>> factors = new ArrayList<>();
        double[] scores = {sWatch, sSubmit, sQuiz, sLogin, sDiscuss};
        double[] weights = {w1.doubleValue(), w2.doubleValue(), w3.doubleValue(), w4.doubleValue(), w5.doubleValue()};
        double[] raws = {watchRate, submitRate, quizRate, loginDays * 20.0, effective};
        for (int i = 0; i < 5; i++) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("name", names[i]);
            f.put("weight", Math.round(weights[i] * 100));
            f.put("raw", Math.round(raws[i] * 100) / 100.0);
            f.put("score", Math.round(scores[i] * 100) / 100.0);
            f.put("loss", Math.round(losses[i] * 100) / 100.0);
            factors.add(f);
        }
        res.put("factors", factors);
        trace.log("学习预警", "计算风险", s.getSnapshotNo(), "风险分 " + s.getRiskScore() + "，等级 " + level);
        return res;
    }

    private BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private String nameOf(Long uid) {
        SysUser u = uid == null ? null : userMapper.selectById(uid);
        return u == null ? "" : u.getRealName();
    }

    public PageResult<Map<String, Object>> snapshotPage(String riskLevel, Long courseId, Long studentId,
                                                        long page, long size) {
        QueryWrapper<RiskSnapshot> w = new QueryWrapper<RiskSnapshot>().eq("deleted", 0);
        if (riskLevel != null && !riskLevel.isBlank()) {
            w.eq("risk_level", riskLevel);
        }
        if (courseId != null) {
            w.eq("course_id", courseId);
        }
        if (studentId != null) {
            w.eq("student_id", studentId);
        }
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
        w.orderByDesc("calc_time");
        Page<RiskSnapshot> p = snapMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (RiskSnapshot s : p.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("snapshotNo", s.getSnapshotNo());
            m.put("studentId", s.getStudentId());
            m.put("studentName", nameOf(s.getStudentId()));
            SysUser stu = userMapper.selectById(s.getStudentId());
            m.put("deptName", stu == null ? "" : stu.getDeptName());
            m.put("courseId", s.getCourseId());
            Course c = courseMapper.selectById(s.getCourseId());
            m.put("courseName", c == null ? "" : c.getCourseName());
            m.put("ruleVersion", s.getRuleVersion());
            m.put("scoreWatch", s.getScoreWatch());
            m.put("scoreSubmit", s.getScoreSubmit());
            m.put("scoreQuiz", s.getScoreQuiz());
            m.put("scoreLogin", s.getScoreLogin());
            m.put("scoreDiscuss", s.getScoreDiscuss());
            m.put("riskScore", s.getRiskScore());
            m.put("riskLevel", s.getRiskLevel());
            m.put("gapText", s.getGapText());
            m.put("suggestion", s.getSuggestion());
            m.put("missingFlag", s.getMissingFlag());
            m.put("adoptFlag", s.getAdoptFlag());
            m.put("calcTime", s.getCalcTime());
            m.put("corrected", corrMapper.selectCount(new QueryWrapper<RiskCorrection>()
                    .eq("snapshot_id", s.getId())) > 0);
            rows.add(m);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    public Map<String, Object> snapshotDetail(Long id) {
        RiskSnapshot s = snapMapper.selectById(id);
        if (s == null) {
            throw new BizException(404, "预警记录不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("snapshot", s);
        m.put("studentName", nameOf(s.getStudentId()));
        Course c = courseMapper.selectById(s.getCourseId());
        m.put("courseName", c == null ? "" : c.getCourseName());
        m.put("corrections", corrMapper.selectList(new QueryWrapper<RiskCorrection>()
                .eq("snapshot_id", id).orderByDesc("correct_time")));
        return m;
    }

    /** 人工修正：原因不少于 5 字，原始结果保留不覆盖 */
    @Transactional
    public boolean correct(Long id, String newLevel, String reason, Double newScore) {
        RiskSnapshot s = snapMapper.selectById(id);
        if (s == null) {
            throw new BizException(404, "预警记录不存在");
        }
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("修正原因不少于 5 字");
        }
        RiskCorrection c = new RiskCorrection();
        c.setSnapshotId(id);
        c.setOriginLevel(s.getRiskLevel());
        c.setOriginScore(s.getRiskScore());
        c.setNewLevel(newLevel);
        c.setNewScore(newScore == null ? s.getRiskScore() : bd(newScore));
        c.setReason(reason);
        c.setCorrectBy(UserContext.userId());
        c.setCorrectTime(LocalDateTime.now());
        c.setCreateTime(LocalDateTime.now());
        corrMapper.insert(c);
        trace.log("学习预警", "人工修正", s.getSnapshotNo(),
                "由 " + s.getRiskLevel() + " 修正为 " + newLevel + "，原因：" + reason);
        return true;
    }

    /** 采纳 / 不采纳推荐结论，用于统计推荐采纳率 */
    @Transactional
    public boolean adopt(Long id, boolean adopted) {
        RiskSnapshot s = new RiskSnapshot();
        s.setId(id);
        s.setAdoptFlag(adopted ? 1 : 0);
        return snapMapper.updateById(s) > 0;
    }

    /** 规则效果统计：推荐采纳率、人工修正率、各等级人数 */
    public Map<String, Object> stats(Long courseId) {
        QueryWrapper<RiskSnapshot> w = new QueryWrapper<RiskSnapshot>().eq("deleted", 0);
        if (courseId != null) {
            w.eq("course_id", courseId);
        }
        List<RiskSnapshot> list = snapMapper.selectList(w);
        long total = list.size();
        long adopted = list.stream().filter(x -> x.getAdoptFlag() != null && x.getAdoptFlag() == 1).count();
        long corrected = corrMapper.selectCount(new QueryWrapper<RiskCorrection>());
        Map<String, Long> byLevel = new LinkedHashMap<>();
        for (String lv : List.of("低风险", "关注", "中风险", "高风险")) {
            byLevel.put(lv, list.stream().filter(x -> lv.equals(x.getRiskLevel())).count());
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", total);
        m.put("adopted", adopted);
        m.put("adoptRate", total == 0 ? 0 : Math.round(adopted * 1000.0 / total) / 10.0);
        m.put("corrected", corrected);
        m.put("correctRate", total == 0 ? 0 : Math.round(corrected * 1000.0 / total) / 10.0);
        m.put("byLevel", byLevel);
        m.put("avgScore", total == 0 ? 0
                : Math.round(list.stream().mapToDouble(x -> x.getRiskScore().doubleValue()).average().orElse(0) * 100) / 100.0);
        return m;
    }
}
