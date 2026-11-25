package com.grad.course.controller;

import com.grad.course.common.PageResult;
import com.grad.course.common.R;
import com.grad.course.entity.SysClass;
import com.grad.course.entity.SysDictData;
import com.grad.course.entity.SysMessage;
import com.grad.course.entity.SysNotice;
import com.grad.course.entity.SysOperLog;
import com.grad.course.entity.SysRole;
import com.grad.course.entity.SysTerm;
import com.grad.course.service.DashboardService;
import com.grad.course.service.DiscussionService;
import com.grad.course.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 统计看板、讨论区、基础数据与系统管理 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;
    private final DashboardService dashboardService;
    private final DiscussionService discussionService;

    // ---------- 看板 ----------
    @GetMapping("/dashboard/overview")
    public R<Map<String, Object>> overview() {
        return R.ok(dashboardService.overview());
    }

    @GetMapping("/dashboard/charts")
    public R<Map<String, Object>> charts() {
        return R.ok(dashboardService.charts());
    }

    // ---------- 讨论区 ----------
    @GetMapping("/discussion/page")
    public R<PageResult<Map<String, Object>>> posts(@RequestParam(required = false) Long courseId,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Boolean onlySolved,
                                                    @RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size) {
        return R.ok(discussionService.page(courseId, keyword, onlySolved, page, size));
    }

    @GetMapping("/discussion/{id}")
    public R<Map<String, Object>> post(@PathVariable Long id) {
        return R.ok(discussionService.detail(id));
    }

    @PostMapping("/discussion")
    public R<Long> createPost(@RequestBody Map<String, Object> body) {
        return R.ok(discussionService.create(Long.parseLong(String.valueOf(body.get("courseId"))),
                String.valueOf(body.get("title")), String.valueOf(body.get("content")),
                body.get("chapterId") == null ? null : Long.parseLong(String.valueOf(body.get("chapterId")))));
    }

    @PostMapping("/discussion/{id}/reply")
    public R<Long> reply(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean adopted = Boolean.parseBoolean(String.valueOf(body.getOrDefault("adopted", false)));
        return R.ok(discussionService.reply(id, String.valueOf(body.get("content")), adopted));
    }

    @PostMapping("/discussion/{id}/mark")
    public R<Boolean> mark(@PathVariable Long id, @RequestParam String type,
                           @RequestParam(defaultValue = "true") boolean value) {
        return R.ok(discussionService.mark(id, type, value));
    }

    // ---------- 基础数据 ----------
    @GetMapping("/meta/term/list")
    public R<List<SysTerm>> terms() {
        return R.ok(systemService.terms());
    }

    @GetMapping("/meta/class/page")
    public R<PageResult<Map<String, Object>>> classes(@RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) Long termId,
                                                      @RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "10") long size) {
        return R.ok(systemService.classPage(keyword, termId, page, size));
    }

    @PostMapping("/meta/class")
    public R<Long> saveClass(@RequestBody SysClass c) {
        return R.ok(systemService.saveClass(c));
    }

    @PutMapping("/meta/class/{id}")
    public R<Long> updateClass(@PathVariable Long id, @RequestBody SysClass c) {
        c.setId(id);
        return R.ok(systemService.saveClass(c));
    }

    @GetMapping("/meta/dict/{type}")
    public R<List<SysDictData>> dict(@PathVariable String type) {
        return R.ok(systemService.dict(type));
    }

    @GetMapping("/system/dict/types")
    public R<List<Map<String, Object>>> dictTypes() {
        return R.ok(systemService.dictTypes());
    }

    @GetMapping("/system/dict/data")
    public R<List<SysDictData>> dictData(@RequestParam String type) {
        return R.ok(systemService.dict(type));
    }

    @PostMapping("/system/dict/data")
    public R<Long> saveDictData(@RequestBody Map<String, Object> body) {
        return R.ok(systemService.saveDictData(body));
    }

    @GetMapping("/system/notice/list")
    public R<List<SysNotice>> notices() {
        return R.ok(systemService.notices());
    }

    @GetMapping("/system/message/page")
    public R<PageResult<SysMessage>> messages(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) Integer readFlag) {
        return R.ok(systemService.messagePage(page, size, readFlag));
    }

    @PostMapping("/system/message/{id}/read")
    public R<Boolean> read(@PathVariable Long id) {
        return R.ok(systemService.readMessage(id));
    }

    @PostMapping("/system/message/read-all")
    public R<Boolean> readAll() {
        return R.ok(systemService.readAll());
    }

    // ---------- 用户 / 角色 / 菜单 / 日志 ----------
    @GetMapping("/system/user/page")
    public R<PageResult<Map<String, Object>>> users(@RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Long roleId,
                                                    @RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size) {
        return R.ok(systemService.userPage(keyword, roleId, page, size));
    }

    @PostMapping("/system/user")
    public R<Long> saveUser(@RequestBody Map<String, Object> body) {
        return R.ok(systemService.saveUser(body));
    }

    @PutMapping("/system/user/{id}")
    public R<Long> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return R.ok(systemService.saveUser(body));
    }

    @GetMapping("/system/role/list")
    public R<List<SysRole>> roles() {
        return R.ok(systemService.roles());
    }

    @GetMapping("/system/role/{id}/menus")
    public R<List<Long>> roleMenus(@PathVariable Long id) {
        return R.ok(systemService.roleMenus(id));
    }

    @GetMapping("/system/menu/tree")
    public R<List<Map<String, Object>>> menuTree() {
        return R.ok(systemService.menuTreeForAssign());
    }

    @PostMapping("/system/role/{id}/menus")
    public R<Boolean> saveRoleMenus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object ids = body.get("menuIds");
        List<Long> list = new java.util.ArrayList<>();
        if (ids instanceof List<?> l) {
            for (Object o : l) {
                if (o != null) {
                    list.add(Long.parseLong(String.valueOf(o)));
                }
            }
        }
        return R.ok(systemService.saveRoleMenus(id, list));
    }

    @GetMapping("/system/log/page")
    public R<PageResult<SysOperLog>> logs(@RequestParam(required = false) String keyword,
                                          @RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size) {
        return R.ok(systemService.logPage(keyword, page, size));
    }
}
