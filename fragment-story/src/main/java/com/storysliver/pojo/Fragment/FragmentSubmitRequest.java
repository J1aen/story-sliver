package com.storysliver.pojo.Fragment;

/**
 * 发布请求体（Task 7，待实现）。
 * 字段：
 *   - content 碎片内容（@NotBlank @Size(max=1000)）
 *   - isAnonymous 是否匿名（Boolean，默认 false）
 * 干什么用：@RequestBody 接收发布表单，校验注解在进 Service 前拦截空值/超长。
 */
public class FragmentSubmitRequest {
}
