package com.grad.course.controller;

import com.grad.course.common.PageResult;
import com.grad.course.common.R;
import com.grad.course.entity.ExamQuestion;
import com.grad.course.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 题库、考试、答题、阅卷与成绩 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    // ---------- 题库 ----------
    @GetMapping("/exam-question/page")
    public R<PageResult<Map<String, Object>>> questions(@RequestParam(required = false) Long courseId,
                                                        @RequestParam(required = false) String questionType,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "10") long size) {
        return R.ok(examService.questionPage(courseId, questionType, keyword, page, size));
    }

    @PostMapping("/exam-question")
    public R<Long> saveQuestion(@RequestBody ExamQuestion q) {
        return R.ok(examService.saveQuestion(q));
    }

    @PutMapping("/exam-question/{id}")
    public R<Long> updateQuestion(@PathVariable Long id, @RequestBody ExamQuestion q) {
        q.setId(id);
        return R.ok(examService.saveQuestion(q));
    }

    @DeleteMapping("/exam-question/{id}")
    public R<Boolean> deleteQuestion(@PathVariable Long id) {
        return R.ok(examService.deleteQuestion(id));
    }

    // ---------- 考试 ----------
    @GetMapping("/exam/page")
    public R<PageResult<Map<String, Object>>> exams(@RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) Long courseId,
                                                    @RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size) {
        return R.ok(examService.examPage(keyword, status, courseId, page, size));
    }

    @GetMapping("/exam/{id}")
    public R<Map<String, Object>> exam(@PathVariable Long id) {
        return R.ok(examService.examDetail(id));
    }

    @PostMapping("/exam")
    public R<Long> createExam(@RequestBody Map<String, Object> body) {
        return R.ok(examService.saveExam(body, longList(body.get("questionIds"))));
    }

    @PutMapping("/exam/{id}")
    public R<Long> updateExam(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return R.ok(examService.saveExam(body, longList(body.get("questionIds"))));
    }

    @PostMapping("/exam/{id}/audit")
    public R<Boolean> audit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean pass = Boolean.parseBoolean(String.valueOf(body.getOrDefault("pass", false)));
        return R.ok(examService.audit(id, pass, body.get("opinion") == null ? null : String.valueOf(body.get("opinion"))));
    }

    @PostMapping("/exam/{id}/start")
    public R<Map<String, Object>> start(@PathVariable Long id) {
        return R.ok(examService.start(id));
    }

    @PostMapping("/exam/{id}/publish-score")
    public R<Map<String, Object>> publish(@PathVariable Long id) {
        return R.ok(examService.publishScore(id));
    }

    // ---------- 答题 ----------
    @PostMapping("/exam-record/{id}/submit")
    public R<Map<String, Object>> submit(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Map<Long, String> answers = new java.util.LinkedHashMap<>();
        Object a = body == null ? null : body.get("answers");
        if (a instanceof Map<?, ?> mp) {
            for (Map.Entry<?, ?> e : mp.entrySet()) {
                answers.put(Long.parseLong(String.valueOf(e.getKey())),
                        e.getValue() == null ? null : String.valueOf(e.getValue()));
            }
        }
        return R.ok(examService.submit(id, answers));
    }

    @PostMapping("/exam-record/{id}/cheat-event")
    public R<Map<String, Object>> cheat(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                        jakarta.servlet.http.HttpServletRequest req) {
        return R.ok(examService.cheatEvent(id,
                body.get("eventType") == null ? "切屏" : String.valueOf(body.get("eventType")),
                body.get("device") == null ? "未知设备" : String.valueOf(body.get("device")),
                req.getRemoteAddr()));
    }

    @GetMapping("/exam-record/grading")
    public R<PageResult<Map<String, Object>>> grading(@RequestParam(required = false) Long examId,
                                                     @RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size) {
        return R.ok(examService.gradingList(examId, page, size));
    }

    @GetMapping("/exam-record/{id}")
    public R<Map<String, Object>> record(@PathVariable Long id) {
        return R.ok(examService.recordDetail(id));
    }

    @PostMapping("/exam-record/{id}/grade")
    public R<Map<String, Object>> grade(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object items = body.get("items");
        List<Map<String, Object>> list = new ArrayList<>();
        if (items instanceof List<?> l) {
            for (Object o : l) {
                if (o instanceof Map<?, ?> m) {
                    Map<String, Object> item = new java.util.LinkedHashMap<>();
                    m.forEach((k, v) -> item.put(String.valueOf(k), v));
                    list.add(item);
                }
            }
        }
        return R.ok(examService.grade(id, list));
    }

    // ---------- 成绩 ----------
    @GetMapping("/score/page")
    public R<PageResult<Map<String, Object>>> scores(@RequestParam(required = false) Long courseId,
                                                    @RequestParam(required = false) Long studentId,
                                                    @RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size) {
        return R.ok(examService.scorePage(courseId, studentId, page, size));
    }

    private List<Long> longList(Object o) {
        List<Long> ids = new ArrayList<>();
        if (o instanceof List<?> l) {
            for (Object x : l) {
                if (x != null) {
                    ids.add(Long.parseLong(String.valueOf(x)));
                }
            }
        }
        return ids;
    }
}
