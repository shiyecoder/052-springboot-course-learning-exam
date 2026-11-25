package com.grad.course.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.course.common.BizException;
import com.grad.course.common.PageResult;
import com.grad.course.common.UserContext;
import com.grad.course.entity.*;
import com.grad.course.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统与基础数据：学期、教学班、字典、用户、角色、菜单、日志、公告、站内消息。
 */
@Service
@RequiredArgsConstructor
public class SystemService {

    private final SysTermMapper termMapper;
    private final SysClassMapper classMapper;
    private final SysDictDataMapper dictDataMapper;
    private final SysDictTypeMapper dictTypeMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysOperLogMapper logMapper;
    private final SysNoticeMapper noticeMapper;
    private final SysMessageMapper messageMapper;
    private final PasswordEncoder encoder;
    private final TraceService trace;

    public List<SysTerm> terms() {
        return termMapper.selectList(new QueryWrapper<SysTerm>().eq("deleted", 0).orderByDesc("start_date"));
    }

    public PageResult<Map<String, Object>> classPage(String keyword, Long termId, long page, long size) {
        QueryWrapper<SysClass> w = new QueryWrapper<SysClass>().eq("deleted", 0);
        if (keyword != null && !keyword.isBlank()) {
            w.and(x -> x.like("class_name", keyword).or().like("class_no", keyword));
        }
        if (termId != null) {
            w.eq("term_id", termId);
        }
        w.orderByDesc("create_time");
        Page<SysClass> p = classMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getTotal(), page, size, p.getRecords().stream().map(this::classVo).toList());
    }

    private Map<String, Object> classVo(SysClass c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("classNo", c.getClassNo());
        m.put("className", c.getClassName());
        m.put("courseId", c.getCourseId());
        m.put("teacherId", c.getTeacherId());
        m.put("teacherName", nameOf(c.getTeacherId()));
        m.put("termId", c.getTermId());
        m.put("major", c.getMajor());
        m.put("studentCount", c.getStudentCount());
        m.put("capacity", c.getCapacity());
        m.put("schedule", c.getSchedule());
        m.put("status", c.getStatus());
        return m;
    }

    public String nameOf(Long uid) {
        if (uid == null) {
            return "";
        }
        SysUser u = userMapper.selectById(uid);
        return u == null ? "" : u.getRealName();
    }

    @Transactional
    public Long saveClass(SysClass c) {
        if (c.getId() == null) {
            c.setClassNo("BJ" + System.currentTimeMillis() % 1000000);
            c.setCreateTime(LocalDateTime.now());
            classMapper.insert(c);
            trace.log("课程管理", "新增教学班", c.getClassNo(), "新增教学班 " + c.getClassName());
            return c.getId();
        }
        classMapper.updateById(c);
        return c.getId();
    }

    public List<SysDictData> dict(String type) {
        return dictDataMapper.selectList(new QueryWrapper<SysDictData>()
                .eq("dict_type", type).eq("status", 1).orderByAsc("sort_no"));
    }

    /** 字典类型清单（带条目数），供字典维护页左栏使用 */
    public List<Map<String, Object>> dictTypes() {
        List<SysDictType> types = dictTypeMapper.selectList(new QueryWrapper<SysDictType>()
                .eq("deleted", 0).orderByAsc("id"));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysDictType t : types) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("dictName", t.getDictName());
            m.put("dictType", t.getDictType());
            m.put("remark", t.getRemark());
            m.put("count", dictDataMapper.selectCount(new QueryWrapper<SysDictData>()
                    .eq("dict_type", t.getDictType()).eq("deleted", 0)));
            rows.add(m);
        }
        return rows;
    }

    /** 字典项保存：有 id 则更新，无 id 则新增 */
    @Transactional
    public Long saveDictData(Map<String, Object> body) {
        Object idObj = body.get("id");
        String label = body.get("dictLabel") == null ? "" : String.valueOf(body.get("dictLabel")).trim();
        String type = body.get("dictType") == null ? "" : String.valueOf(body.get("dictType")).trim();
        if (label.isEmpty()) {
            throw new BizException("字典标签不能为空");
        }
        if (type.isEmpty()) {
            throw new BizException("字典类型不能为空");
        }
        SysDictData d = new SysDictData();
        d.setDictType(type);
        d.setDictLabel(label);
        d.setDictValue(body.get("dictValue") == null ? label : String.valueOf(body.get("dictValue")));
        d.setTagType(body.get("tagType") == null ? null : String.valueOf(body.get("tagType")));
        d.setSortNo(body.get("sortNo") == null ? 10 : Integer.parseInt(String.valueOf(body.get("sortNo"))));
        d.setStatus(body.get("status") == null ? 1 : Integer.parseInt(String.valueOf(body.get("status"))));
        if (idObj == null || String.valueOf(idObj).isBlank()) {
            d.setDeleted(0);
            d.setCreateTime(LocalDateTime.now());
            dictDataMapper.insert(d);
        } else {
            d.setId(Long.parseLong(String.valueOf(idObj)));
            dictDataMapper.updateById(d);
        }
        trace.log("字典维护", "保存", type, "保存字典项 " + type + ":" + label);
        return d.getId();
    }

    // ---------- 用户 ----------
    public PageResult<Map<String, Object>> userPage(String keyword, Long roleId, long page, long size) {
        QueryWrapper<SysUser> w = new QueryWrapper<SysUser>().eq("deleted", 0);
        if (keyword != null && !keyword.isBlank()) {
            w.and(x -> x.like("real_name", keyword).or().like("username", keyword).or().like("user_no", keyword));
        }
        if (roleId != null) {
            List<Long> uids = userRoleMapper.selectList(new QueryWrapper<SysUserRole>().eq("role_id", roleId))
                    .stream().map(SysUserRole::getUserId).toList();
            if (uids.isEmpty()) {
                return PageResult.of(0, page, size, List.of());
            }
            w.in("id", uids);
        }
        w.orderByAsc("id");
        Page<SysUser> p = userMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysUser u : p.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("userNo", u.getUserNo());
            m.put("phone", u.getPhone());
            m.put("email", u.getEmail());
            m.put("avatar", u.getAvatar());
            m.put("gender", u.getGender());
            m.put("deptName", u.getDeptName());
            m.put("title", u.getTitle());
            m.put("status", u.getStatus());
            m.put("creditScore", u.getCreditScore());
            m.put("lastLoginTime", u.getLastLoginTime());
            m.put("createTime", u.getCreateTime());
            SysUserRole ur = userRoleMapper.selectOne(
                    new QueryWrapper<SysUserRole>().eq("user_id", u.getId()).last("limit 1"));
            SysRole r = ur == null ? null : roleMapper.selectById(ur.getRoleId());
            m.put("roleId", r == null ? null : r.getId());
            m.put("roleName", r == null ? "" : r.getRoleName());
            rows.add(m);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    @Transactional
    public Long saveUser(Map<String, Object> b) {
        Object idObj = b.get("id");
        SysUser u = new SysUser();
        u.setRealName(str(b.get("realName")));
        u.setPhone(str(b.get("phone")));
        u.setEmail(str(b.get("email")));
        u.setDeptName(str(b.get("deptName")));
        u.setTitle(str(b.get("title")));
        u.setGender(str(b.get("gender")));
        u.setAvatar(str(b.get("avatar")));
        u.setStatus(b.get("status") == null ? 1 : Integer.parseInt(str(b.get("status"))));
        Long roleId = b.get("roleId") == null ? null : Long.parseLong(str(b.get("roleId")));
        if (idObj == null) {
            String username = str(b.get("username"));
            if (username == null || username.isBlank()) {
                throw new BizException("登录账号不能为空");
            }
            if (userMapper.selectCount(new QueryWrapper<SysUser>().eq("username", username)) > 0) {
                throw new BizException("登录账号已存在");
            }
            u.setUsername(username);
            u.setUserNo(b.get("userNo") == null ? "U" + System.currentTimeMillis() % 100000 : str(b.get("userNo")));
            u.setPassword(encoder.encode(str(b.get("password")) == null ? "123456" : str(b.get("password"))));
            u.setCreditScore(100);
            u.setCreateTime(LocalDateTime.now());
            userMapper.insert(u);
        } else {
            u.setId(Long.parseLong(str(idObj)));
            userMapper.updateById(u);
        }
        if (roleId != null) {
            userRoleMapper.delete(new QueryWrapper<SysUserRole>().eq("user_id", u.getId()));
            SysUserRole ur = new SysUserRole();
            ur.setUserId(u.getId());
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
        trace.log("系统管理", idObj == null ? "新增用户" : "修改用户", u.getUsername(), "维护用户 " + u.getRealName());
        return u.getId();
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    // ---------- 角色 / 菜单 ----------
    public List<SysRole> roles() {
        return roleMapper.selectList(new QueryWrapper<SysRole>().eq("deleted", 0).orderByAsc("id"));
    }

    public List<Long> roleMenus(Long roleId) {
        return roleMenuMapper.selectList(new QueryWrapper<SysRoleMenu>().eq("role_id", roleId))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }

    /**
     * 分配菜单用的完整菜单树。
     * 需求第 12 条：菜单管理自身不应出现在「分配菜单」里，避免角色自己改权限；
     * 因此 nodeId=20 的「菜单管理」及其按钮被剔除。
     */
    public List<Map<String, Object>> menuTreeForAssign() {
        List<SysMenu> all = menuMapper.selectList(new QueryWrapper<SysMenu>()
                .eq("deleted", 0).ne("id", 20L).orderByAsc("sort_no"));
        Map<Long, List<SysMenu>> byParent = all.stream().collect(Collectors.groupingBy(SysMenu::getParentId));
        List<Map<String, Object>> tree = new ArrayList<>();
        for (SysMenu m : byParent.getOrDefault(0L, List.of())) {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", m.getId());
            n.put("label", m.getMenuName());
            n.put("menuType", m.getMenuType());
            n.put("perms", m.getPerms());
            n.put("path", m.getPath());
            n.put("icon", m.getIcon());
            n.put("sortNo", m.getSortNo());
            List<Map<String, Object>> ch = new ArrayList<>();
            for (SysMenu c : byParent.getOrDefault(m.getId(), List.of())) {
                Map<String, Object> cc = new LinkedHashMap<>();
                cc.put("id", c.getId());
                cc.put("label", c.getMenuName());
                cc.put("menuType", c.getMenuType());
                cc.put("perms", c.getPerms());
                cc.put("path", c.getPath());
                cc.put("icon", c.getIcon());
                cc.put("sortNo", c.getSortNo());
                ch.add(cc);
            }
            n.put("children", ch);
            tree.add(n);
        }
        return tree;
    }

    @Transactional
    public boolean saveRoleMenus(Long roleId, List<Long> menuIds) {
        if (roleId == null) {
            throw new BizException("角色不能为空");
        }
        if (roleId == 1L) {
            throw new BizException("系统管理员默认拥有全部菜单，不允许调整");
        }
        roleMenuMapper.delete(new QueryWrapper<SysRoleMenu>().eq("role_id", roleId));
        for (Long mid : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(mid);
            roleMenuMapper.insert(rm);
        }
        trace.log("系统管理", "分配菜单", "ROLE-" + roleId, "为角色 " + roleId + " 重新分配 " + menuIds.size() + " 个菜单");
        return true;
    }

    // ---------- 日志 / 公告 / 消息 ----------
    public PageResult<SysOperLog> logPage(String keyword, long page, long size) {
        QueryWrapper<SysOperLog> w = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.and(x -> x.like("module", keyword).or().like("oper_name", keyword).or().like("biz_no", keyword));
        }
        w.orderByDesc("oper_time");
        Page<SysOperLog> p = logMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getTotal(), page, size, p.getRecords());
    }

    public List<SysNotice> notices() {
        return noticeMapper.selectList(new QueryWrapper<SysNotice>().eq("deleted", 0)
                .eq("status", "已发布").orderByDesc("publish_time"));
    }

    public PageResult<SysMessage> messagePage(long page, long size, Integer readFlag) {
        QueryWrapper<SysMessage> w = new QueryWrapper<SysMessage>()
                .eq("receiver_id", UserContext.userId()).eq("deleted", 0);
        if (readFlag != null) {
            w.eq("read_flag", readFlag);
        }
        w.orderByDesc("create_time");
        Page<SysMessage> p = messageMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getTotal(), page, size, p.getRecords());
    }

    @Transactional
    public boolean readMessage(Long id) {
        SysMessage m = new SysMessage();
        m.setId(id);
        m.setReadFlag(1);
        return messageMapper.updateById(m) > 0;
    }

    /** 批量已读：消息中心「全部已读」 */
    @Transactional
    public boolean readAll() {
        SysMessage m = new SysMessage();
        m.setReadFlag(1);
        return messageMapper.update(m, new QueryWrapper<SysMessage>()
                .eq("receiver_id", UserContext.userId()).eq("read_flag", 0)) >= 0;
    }
}
