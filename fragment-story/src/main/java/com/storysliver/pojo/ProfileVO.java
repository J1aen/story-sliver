package com.storysliver.pojo;

import lombok.Data;

/**
 * 他人主页展示对象（v2.0 Task 20）。
 * 干什么用：公开主页接口返回「用户公开信息 + 该用户非匿名已发布碎片分页」。
 * 为什么只放展示字段：绝不把 password/email 等敏感字段带出去。
 */
@Data
public class ProfileVO {
    private Long userId;//用户 id（前端跳转 /profile/:userId 用）
    private String nickname;//昵称（主页显示 @昵称）
    private String avatar;//已审核头像 URL（可空）
    private Integer role;//角色（前端渲染站长/管理员铭牌）
    private PageBean fragments;//该用户「非匿名且已发布」碎片分页（total + list）
}
