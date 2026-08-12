package com.storysliver.pojo.Fragment;

/**
 * 碎片展示对象（Task 7，待实现）。
 * 字段：
 *   - id、content、likeCount、isAnonymous、status
 *   - authorName 展示名（isAnonymous==1 时显示「匿名用户」，否则显示昵称）
 *   - likedByMe 当前用户是否已赞、createdAt 格式化时间
 * 为什么用 VO 而不是直接返回实体：不把 user_id 等敏感字段暴露给前端，还能拼好展示名。
 */
public class FragmentVO {
}
