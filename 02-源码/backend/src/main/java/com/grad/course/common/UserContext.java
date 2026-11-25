package com.grad.course.common;

/**
 * 当前登录用户上下文。由 JWT 过滤器写入，Controller / Service 只读。
 * 数据范围按需求文档「权限与消息规则」三级控制：ALL 全域、DEPT 本部门、SELF 本人。
 */
public class UserContext {

    public record CurrentUser(Long userId, String username, String realName, String roleCode,
                              Long orgId, String orgName, String dataScope) {
    }

    /** 角色编码常量，与 sys_role.role_code 一致 */
    public static final String ROLE_ADMIN = "ADMIN";           // 系统管理员
    public static final String ROLE_JW = "JWADMIN";            // 教务管理员
    public static final String ROLE_TEACHER = "TEACHER";       // 任课教师
    public static final String ROLE_ASSISTANT = "ASSISTANT";   // 助教
    public static final String ROLE_STUDENT = "STUDENT";       // 学生

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.userId();
    }

    public static String username() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.username();
    }

    public static String realName() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.realName();
    }

    public static String roleCode() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.roleCode();
    }

    public static Long orgId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.orgId();
    }

    public static String orgName() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.orgName();
    }

    public static String dataScope() {
        CurrentUser u = HOLDER.get();
        return u == null ? "SELF" : u.dataScope();
    }

    public static boolean isAdmin() {
        return ROLE_ADMIN.equals(roleCode());
    }

    /** 教务管理员：开课审核、学期与选课规则、成绩复核、统计导出 */
    public static boolean isJw() {
        return ROLE_JW.equals(roleCode()) || isAdmin();
    }

    /** 任课教师：本人课程、考试与阅卷 */
    public static boolean isTeacher() {
        return ROLE_TEACHER.equals(roleCode());
    }

    /** 助教：协助批阅与讨论区维护 */
    public static boolean isAssistant() {
        return ROLE_ASSISTANT.equals(roleCode());
    }

    public static boolean isStudent() {
        return ROLE_STUDENT.equals(roleCode());
    }

    /** 教学人员（教师或助教），可进入阅卷台 */
    public static boolean isStaffOfTeaching() {
        return isTeacher() || isAssistant();
    }

    /** 是否按部门过滤（部门负责人） */
    public static boolean scopeDept() {
        return "DEPT".equals(dataScope());
    }

    public static void clear() {
        HOLDER.remove();
    }
}
