package com.storysliver.pojo.Fragment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发布请求体：承载发布表单的字段。
 * 干什么用：@RequestBody 接收发布表单，校验注解在进 Service 前拦截空值/超长。
 * 为什么在字段上加校验注解：Spring 的 @Valid 会自动执行，Controller 不用手写 if。
 */
@Data
public class FragmentSubmitRequest {
    @NotBlank(message = "内容不能为空")
    @Size(max = 1000, message = "内容不能超过 1000 字")
    private String content;//碎片内容（1000 字封顶）

    private Boolean isAnonymous = false;//是否匿名发布：true 时对外显示「匿名用户」
}
