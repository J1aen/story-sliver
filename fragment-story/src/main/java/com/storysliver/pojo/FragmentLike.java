package com.storysliver.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 点赞实体：对应 fragment_like 表。
 * 干什么用：记录「哪个用户赞过哪条碎片」，支撑每人每条一次、可取消的点赞规则。
 * 为什么这样设计：数据库唯一约束 (fragment_id, user_id) 从底层保证不能重复点赞，比应用层判断更可靠。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FragmentLike {
    private Long id;//主键
    private Long fragmentId;//被点赞的碎片id
    private Long userId;//点赞用户id
    private LocalDateTime createdAt;//点赞时间
}
