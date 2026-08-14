package com.storysliver.pojo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 敏感词实体：对应 sensitive_word 表（v1.2）。
 * 干什么用：注册/修改昵称时做敏感词校验；站长在管理后台维护。
 */
@Data
public class SensitiveWord {
    private Long id;//主键
    private String word;//敏感词内容（唯一）
    private LocalDateTime createdAt;//创建时间
}
