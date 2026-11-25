package com.grad.course.service;

import com.grad.course.common.UserContext;
import com.grad.course.entity.SysOperLog;
import com.grad.course.mapper.SysOperLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 操作留痕：关键写操作统一写 sys_oper_log，供「操作日志」页与合规审计使用。
 * 日志失败不影响主业务（吞异常），避免因日志表锁导致业务回滚。
 */
@Service
@RequiredArgsConstructor
public class TraceService {

    private final SysOperLogMapper logMapper;

    public void log(String module, String operType, String bizNo, String content) {
        try {
            SysOperLog l = new SysOperLog();
            l.setModule(module);
            l.setOperType(operType);
            l.setBizNo(bizNo);
            l.setContent(content);
            l.setOperId(UserContext.userId());
            l.setOperName(UserContext.realName());
            l.setOperTime(LocalDateTime.now());
            l.setIp("127.0.0.1");
            l.setCreateTime(LocalDateTime.now());
            logMapper.insert(l);
        } catch (Exception ignored) {
            // 留痕失败不阻断业务
        }
    }
}
