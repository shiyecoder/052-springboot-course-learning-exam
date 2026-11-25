package com.grad.course.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grad.course.common.R;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 安全配置：无状态 JWT。JwtFilter 把 roleCode 直接写成 authority（不带 ROLE_ 前缀），
 * 因此这里统一用 hasAuthority，避免 hasRole 自动加前缀导致全员 403。
 * 关键写接口按角色分组放行，前端隐藏按钮不等于授权，后端必须复核。
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/login", "/uploads/**", "/error").permitAll()
                // 消息中心：所有登录用户只能读写自己的消息
                .requestMatchers("/api/system/message/**").authenticated()
                // 公告列表：登录用户都可读（门户首页展示）
                .requestMatchers(HttpMethod.GET, "/api/system/notice/list").authenticated()
                .requestMatchers("/api/auth/**").authenticated()
                // 系统管理（用户 / 角色 / 菜单 / 字典 / 日志）：仅系统管理员
                .requestMatchers("/api/system/**").hasAuthority("ADMIN")

                // 开课审核、考试审核：教务管理员
                .requestMatchers(HttpMethod.POST, "/api/course/*/audit", "/api/exam/*/audit")
                        .hasAnyAuthority("ADMIN", "JWADMIN")
                // 课程维护：教师与管理员
                .requestMatchers(HttpMethod.POST, "/api/course", "/api/course/**")
                        .hasAnyAuthority("ADMIN", "TEACHER", "JWADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/course", "/api/course/**")
                        .hasAnyAuthority("ADMIN", "TEACHER", "JWADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/course", "/api/course/**")
                        .hasAnyAuthority("ADMIN", "JWADMIN")
                .requestMatchers(HttpMethod.POST, "/api/course-chapter", "/api/course-chapter/**")
                        .hasAnyAuthority("ADMIN", "TEACHER")
                .requestMatchers(HttpMethod.PUT, "/api/course-chapter", "/api/course-chapter/**")
                        .hasAnyAuthority("ADMIN", "TEACHER")
                .requestMatchers(HttpMethod.DELETE, "/api/course-chapter/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/exam-question", "/api/exam-question/**")
                        .hasAnyAuthority("ADMIN", "TEACHER", "ASSISTANT")
                .requestMatchers(HttpMethod.PUT, "/api/exam-question/**")
                        .hasAnyAuthority("ADMIN", "TEACHER", "ASSISTANT")
                .requestMatchers(HttpMethod.DELETE, "/api/exam-question/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/exam", "/api/exam/*/publish-score")
                        .hasAnyAuthority("ADMIN", "TEACHER")
                .requestMatchers(HttpMethod.PUT, "/api/exam/**").hasAnyAuthority("ADMIN", "TEACHER")
                // 阅卷：教师与助教
                .requestMatchers(HttpMethod.POST, "/api/exam-record/*/grade")
                        .hasAnyAuthority("ADMIN", "TEACHER", "ASSISTANT")
                .requestMatchers(HttpMethod.GET, "/api/exam-record/grading")
                        .hasAnyAuthority("ADMIN", "TEACHER", "ASSISTANT")
                // 预警规则与人工修正：教务管理员
                .requestMatchers(HttpMethod.POST, "/api/risk/rule", "/api/risk/rule/**",
                        "/api/risk/snapshot/*/correct").hasAnyAuthority("ADMIN", "JWADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/risk/rule/**").hasAnyAuthority("ADMIN", "JWADMIN")
                // 讨论区维护（精华 / 已解决）：教师与助教
                .requestMatchers(HttpMethod.POST, "/api/discussion/*/mark")
                        .hasAnyAuthority("ADMIN", "TEACHER", "ASSISTANT")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> write(res, 401, "登录状态已失效，请重新登录"))
                .accessDeniedHandler((req, res, ex) -> write(res, 403, "没有该操作的权限")))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(f -> f.disable())
            .httpBasic(b -> b.disable())
            .logout(l -> l.disable());
        return http.build();
    }

    private void write(jakarta.servlet.http.HttpServletResponse res, int code, String msg) throws java.io.IOException {
        res.setStatus(200);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(objectMapper.writeValueAsString(R.fail(code, msg)));
    }

    private CorsConfigurationSource corsSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }
}
