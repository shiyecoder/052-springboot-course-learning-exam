package com.grad.course.controller;

import com.grad.course.common.PageResult;
import com.grad.course.common.R;
import com.grad.course.entity.CourseChapter;
import com.grad.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 课程、章节、选课与学习进度 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping("/course/page")
    public R<PageResult<Map<String, Object>>> page(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String courseType,
                                                   @RequestParam(required = false) Long termId,
                                                   @RequestParam(required = false) Long teacherId,
                                                   @RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size) {
        return R.ok(courseService.page(keyword, status, courseType, termId, teacherId, page, size));
    }

    @GetMapping("/course/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(courseService.detail(id));
    }

    @PostMapping("/course")
    public R<Long> create(@RequestBody Map<String, Object> body) {
        return R.ok(courseService.saveCourse(body));
    }

    @PutMapping("/course/{id}")
    public R<Long> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return R.ok(courseService.saveCourse(body));
    }

    @DeleteMapping("/course/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(courseService.deleteCourse(id));
    }

    @PostMapping("/course/{id}/submit")
    public R<Boolean> submit(@PathVariable Long id) {
        return R.ok(courseService.submitAudit(id));
    }

    @PostMapping("/course/{id}/audit")
    public R<Boolean> audit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean pass = Boolean.parseBoolean(String.valueOf(body.getOrDefault("pass", false)));
        return R.ok(courseService.audit(id, pass, body.get("opinion") == null ? null : String.valueOf(body.get("opinion"))));
    }

    // ---------- 章节 ----------
    @GetMapping("/course-chapter/page")
    public R<List<Map<String, Object>>> chapters(@RequestParam Long courseId) {
        return R.ok(courseService.chaptersOf(courseId));
    }

    @GetMapping("/course-chapter/{id}")
    public R<Map<String, Object>> chapter(@PathVariable Long id) {
        return R.ok(courseService.chapterDetail(id));
    }

    @PostMapping("/course-chapter")
    public R<Long> saveChapter(@RequestBody CourseChapter c) {
        return R.ok(courseService.saveChapter(c));
    }

    @PutMapping("/course-chapter/{id}")
    public R<Long> updateChapter(@PathVariable Long id, @RequestBody CourseChapter c) {
        c.setId(id);
        return R.ok(courseService.saveChapter(c));
    }

    @DeleteMapping("/course-chapter/{id}")
    public R<Map<String, Object>> deleteChapter(@PathVariable Long id) {
        return R.ok(courseService.deleteChapter(id));
    }

    @PostMapping("/course-chapter/{id}/move")
    public R<Boolean> move(@PathVariable Long id, @RequestParam String direction) {
        return R.ok(courseService.moveChapter(id, direction));
    }

    // ---------- 选课 ----------
    @GetMapping("/enrollment/page")
    public R<PageResult<Map<String, Object>>> enrollPage(@RequestParam(required = false) String status,
                                                         @RequestParam(required = false) Long courseId,
                                                         @RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "10") long size) {
        return R.ok(courseService.enrollPage(null, status, courseId, page, size));
    }

    @GetMapping("/enrollment/my")
    public R<Map<String, Object>> my() {
        return R.ok(courseService.myCourses());
    }

    @PostMapping("/enrollment")
    public R<Map<String, Object>> enroll(@RequestBody Map<String, Object> body) {
        return R.ok(courseService.enroll(Long.parseLong(String.valueOf(body.get("courseId")))));
    }

    @PostMapping("/enrollment/{id}/withdraw")
    public R<Boolean> withdraw(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return R.ok(courseService.withdraw(id, body == null ? null : String.valueOf(body.get("reason"))));
    }

    // ---------- 学习 ----------
    @GetMapping("/lesson/{enrollmentId}/chapters")
    public R<Map<String, Object>> lesson(@PathVariable Long enrollmentId) {
        return R.ok(courseService.lessonView(enrollmentId));
    }

    @PostMapping("/lesson/{enrollmentId}/progress")
    public R<Map<String, Object>> progress(@PathVariable Long enrollmentId, @RequestBody Map<String, Object> body) {
        Long chapterId = Long.parseLong(String.valueOf(body.get("chapterId")));
        int delta = body.get("deltaSeconds") == null ? 15 : Integer.parseInt(String.valueOf(body.get("deltaSeconds")));
        String device = body.get("deviceType") == null ? "PC" : String.valueOf(body.get("deviceType"));
        return R.ok(courseService.reportProgress(enrollmentId, chapterId, delta, device));
    }
}
