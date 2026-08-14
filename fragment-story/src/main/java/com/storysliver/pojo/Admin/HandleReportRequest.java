package com.storysliver.pojo.Admin;

import lombok.Data;

/**
 * 处理举报请求体（Task 21）。
 * 干什么用：接收管理端处理举报时的 JSON：{ action, banDays, banReason }。
 * action 三种：dismiss=不下架 / delete=下架评论 / ban=下架评论并封禁该评论用户。
 */
@Data
public class HandleReportRequest {
    private String action;//处理动作（见类注释）
    private Integer banDays;//封禁天数（ban 时用；空或 ≤0 表示永久封禁）
    private String banReason;//封禁理由（ban 时必填，与现有封禁逻辑一致）
}
