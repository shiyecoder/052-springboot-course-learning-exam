package com.grad.course.controller;

import com.grad.course.common.PageResult;
import com.grad.course.common.R;
import com.grad.course.entity.RiskRule;
import com.grad.course.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 学习预警：规则配置、风险计算、快照查询、人工修正与效果统计 */
@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;

    @GetMapping("/rule/list")
    public R<List<RiskRule>> rules() {
        return R.ok(riskService.rules());
    }

    @GetMapping("/rule/current")
    public R<RiskRule> current() {
        return R.ok(riskService.currentRule());
    }

    @PostMapping("/rule")
    public R<Long> create(@RequestBody RiskRule rule) {
        rule.setId(null);
        return R.ok(riskService.saveRule(rule));
    }

    @PutMapping("/rule/{id}")
    public R<Long> update(@PathVariable Long id, @RequestBody RiskRule rule) {
        rule.setId(id);
        return R.ok(riskService.saveRule(rule));
    }

    /** 计算风险分：入参为选课记录 ID，返回因子明细与解释 */
    @PostMapping("/evaluate")
    public R<Map<String, Object>> evaluate(@RequestBody Map<String, Object> body) {
        return R.ok(riskService.evaluate(Long.parseLong(String.valueOf(body.get("enrollmentId")))));
    }

    @GetMapping("/evaluate/{enrollmentId}")
    public R<Map<String, Object>> evaluateGet(@PathVariable Long enrollmentId) {
        return R.ok(riskService.evaluate(enrollmentId));
    }

    @GetMapping("/snapshot/page")
    public R<PageResult<Map<String, Object>>> snapshots(@RequestParam(required = false) String riskLevel,
                                                        @RequestParam(required = false) Long courseId,
                                                        @RequestParam(required = false) Long studentId,
                                                        @RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "10") long size) {
        return R.ok(riskService.snapshotPage(riskLevel, courseId, studentId, page, size));
    }

    @GetMapping("/snapshot/{id}")
    public R<Map<String, Object>> snapshot(@PathVariable Long id) {
        return R.ok(riskService.snapshotDetail(id));
    }

    @PostMapping("/snapshot/{id}/correct")
    public R<Boolean> correct(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Double score = body.get("newScore") == null ? null : Double.parseDouble(String.valueOf(body.get("newScore")));
        return R.ok(riskService.correct(id, String.valueOf(body.get("newLevel")),
                String.valueOf(body.get("reason")), score));
    }

    @PostMapping("/snapshot/{id}/adopt")
    public R<Boolean> adopt(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean adopted) {
        return R.ok(riskService.adopt(id, adopted));
    }

    @GetMapping("/stats")
    public R<Map<String, Object>> stats(@RequestParam(required = false) Long courseId) {
        return R.ok(riskService.stats(courseId));
    }
}
