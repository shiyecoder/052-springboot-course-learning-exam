package com.grad.course.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.grad.course.entity.*;
import com.grad.course.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 统计看板：8 个核心指标 + 6 张图表（需求第 9 节）。
 * 口径统一按「最近 30 天」统计，图表数据与列表总数保持可对账。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CourseMapper courseMapper;
    private final EnrollmentMapper enrollMapper;
    private final ExamRecordMapper recordMapper;
    private final ExamMapper examMapper;
    private final RiskSnapshotMapper snapMapper;
    private final LessonProgressMapper progressMapper;
    private final CheatLogMapper cheatMapper;
    private final SysUserMapper userMapper;
    private final SysClassMapper classMapper;
    private final CourseChapterMapper chapterMapper;

    public Map<String, Object> overview() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        long onCourse = courseMapper.selectCount(new QueryWrapper<Course>()
                .eq("deleted", 0).in("status", List.of("已发布", "已结课")));
        long newEnroll = enrollMapper.selectCount(new QueryWrapper<Enrollment>()
                .eq("deleted", 0).ge("enroll_time", since));
        List<Enrollment> all = enrollMapper.selectList(new QueryWrapper<Enrollment>()
                .eq("deleted", 0).ne("status", "已退课"));
        double avgProgress = all.isEmpty() ? 0
                : all.stream().mapToDouble(e -> e.getProgressRate() == null ? 0 : e.getProgressRate().doubleValue())
                .average().orElse(0);
        long pendingGrade = recordMapper.selectCount(new QueryWrapper<ExamRecord>().eq("status", "待批阅"));
        long graded = recordMapper.selectCount(new QueryWrapper<ExamRecord>().eq("status", "已出分"));
        long passed = recordMapper.selectCount(new QueryWrapper<ExamRecord>()
                .eq("status", "已出分").eq("pass_flag", 1));
        double passRate = graded == 0 ? 0 : passed * 100.0 / graded;
        long highRisk = snapMapper.selectCount(new QueryWrapper<RiskSnapshot>()
                .eq("deleted", 0).eq("risk_level", "高风险"));
        long chapters = chapterMapper.selectCount(new QueryWrapper<CourseChapter>()
                .eq("deleted", 0).ne("parent_id", 0));
        long totalProgress = progressMapper.selectCount(new QueryWrapper<LessonProgress>());
        long doneProgress = progressMapper.selectCount(new QueryWrapper<LessonProgress>().eq("finish_flag", 1));
        double chapterRate = totalProgress == 0 ? 0 : doneProgress * 100.0 / totalProgress;
        long activeToday = progressMapper.selectList(new QueryWrapper<LessonProgress>()
                        .ge("last_heart_time", LocalDate.now().atStartOfDay()))
                .stream().map(LessonProgress::getEnrollmentId).distinct().count();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("onCourse", onCourse);
        m.put("newEnroll", newEnroll);
        m.put("avgProgress", round(avgProgress));
        m.put("pendingGrade", pendingGrade);
        m.put("passRate", round(passRate));
        m.put("highRisk", highRisk);
        m.put("chapterRate", round(chapterRate));
        m.put("activeToday", activeToday);
        m.put("studentCount", userMapper.selectCount(new QueryWrapper<SysUser>().eq("deleted", 0)));
        m.put("classCount", classMapper.selectCount(new QueryWrapper<SysClass>().eq("deleted", 0)));
        m.put("chapterCount", chapters);
        return m;
    }

    public Map<String, Object> charts() {
        Map<String, Object> m = new LinkedHashMap<>();
        // 1. 各课程学习进度分布（按 0-20/20-40/.../80-100 分桶）
        List<Enrollment> all = enrollMapper.selectList(new QueryWrapper<Enrollment>()
                .eq("deleted", 0).ne("status", "已退课"));
        int[] buckets = new int[5];
        for (Enrollment e : all) {
            double p = e.getProgressRate() == null ? 0 : e.getProgressRate().doubleValue();
            buckets[Math.min(4, (int) (p / 20))]++;
        }
        m.put("progressDist", Map.of("labels", List.of("0-20%", "20-40%", "40-60%", "60-80%", "80-100%"),
                "values", Arrays.stream(buckets).boxed().toList()));

        // 2. 近 30 天日学习时长（心跳秒数按天汇总为分钟）
        Map<LocalDate, Integer> daily = new TreeMap<>();
        for (int i = 29; i >= 0; i--) {
            daily.put(LocalDate.now().minusDays(i), 0);
        }
        List<LessonProgress> plist = progressMapper.selectList(new QueryWrapper<LessonProgress>()
                .ge("last_heart_time", LocalDateTime.now().minusDays(30)));
        for (LessonProgress p : plist) {
            LocalDate d = p.getLastHeartTime().toLocalDate();
            if (daily.containsKey(d)) {
                daily.put(d, daily.get(d) + (p.getWatchedSeconds() == null ? 0 : p.getWatchedSeconds()) / 60);
            }
        }
        m.put("studyTrend", Map.of("labels", daily.keySet().stream().map(LocalDate::toString).toList(),
                "values", daily.values()));

        // 3. 按院系的风险等级堆叠柱
        List<RiskSnapshot> snaps = snapMapper.selectList(new QueryWrapper<RiskSnapshot>().eq("deleted", 0));
        Map<String, Map<String, Long>> stack = new LinkedHashMap<>();
        for (RiskSnapshot s : snaps) {
            SysUser u = userMapper.selectById(s.getStudentId());
            String dept = u == null || u.getDeptName() == null ? "未分配" : u.getDeptName();
            stack.computeIfAbsent(dept, k -> new LinkedHashMap<>()).merge(s.getRiskLevel(), 1L, Long::sum);
        }
        List<String> depts = new ArrayList<>(stack.keySet());
        List<Map<String, Object>> series = new ArrayList<>();
        for (String lv : List.of("低风险", "关注", "中风险", "高风险")) {
            Map<String, Object> se = new LinkedHashMap<>();
            se.put("name", lv);
            se.put("values", depts.stream().map(d -> stack.get(d).getOrDefault(lv, 0L)).toList());
            series.add(se);
        }
        m.put("riskByDept", Map.of("labels", depts, "series", series));

        // 4. 考试成绩分段柱状图
        int[] seg = new int[5];
        for (ExamRecord r : recordMapper.selectList(new QueryWrapper<ExamRecord>().eq("status", "已出分"))) {
            Exam e = examMapper.selectById(r.getExamId());
            double full = e == null || e.getTotalScore() == null ? 60 : e.getTotalScore().doubleValue();
            double rate = r.getTotalScore() == null ? 0 : r.getTotalScore().doubleValue() * 100 / full;
            seg[Math.min(4, (int) (rate / 20))]++;
        }
        m.put("scoreSeg", Map.of("labels", List.of("0-20", "20-40", "40-60", "60-80", "80-100"),
                "values", Arrays.stream(seg).boxed().toList()));

        // 5. 章节观看完成率漏斗（章 → 必修节点完成）
        long chapterTotal = chapterMapper.selectCount(new QueryWrapper<CourseChapter>()
                .eq("deleted", 0).ne("parent_id", 0));
        // 按「节点」去重计数，避免同一节点被多个学生重复统计导致漏斗反向
        long watched = distinctChapters(new QueryWrapper<LessonProgress>().gt("watched_seconds", 0));
        long half = distinctChapters(new QueryWrapper<LessonProgress>().ge("watch_rate", 50));
        long finished = distinctChapters(new QueryWrapper<LessonProgress>().eq("finish_flag", 1));
        m.put("funnel", List.of(
                Map.of("name", "章节节点总数", "value", chapterTotal),
                Map.of("name", "已开始学习", "value", Math.min(chapterTotal, watched)),
                Map.of("name", "观看达 50%", "value", Math.min(chapterTotal, half)),
                Map.of("name", "完成节点", "value", Math.min(chapterTotal, finished))));

        // 6. 防作弊事件次数散点（按考试聚合：事件数 / 参考人数）
        Map<Long, List<CheatLog>> byExam = new LinkedHashMap<>();
        for (CheatLog c : cheatMapper.selectList(new QueryWrapper<CheatLog>())) {
            ExamRecord r = recordMapper.selectById(c.getExamRecordId());
            if (r != null) {
                byExam.computeIfAbsent(r.getExamId(), k -> new ArrayList<>()).add(c);
            }
        }
        List<Map<String, Object>> scatter = new ArrayList<>();
        for (Map.Entry<Long, List<CheatLog>> en : byExam.entrySet()) {
            Exam e = examMapper.selectById(en.getKey());
            long people = recordMapper.selectCount(new QueryWrapper<ExamRecord>().eq("exam_id", en.getKey()));
            scatter.add(Map.of("name", e == null ? ("考试" + en.getKey()) : e.getExamName(),
                    "x", people, "y", en.getValue().size()));
        }
        m.put("cheatScatter", scatter);
        return m;
    }

    private double round(double v) {
        return BigDecimal.valueOf(v).setScale(1, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    /** 命中去重后的章节节点数 */
    private long distinctChapters(QueryWrapper<LessonProgress> w) {
        return progressMapper.selectObjs(w.select("DISTINCT chapter_id")).size();
    }
}
