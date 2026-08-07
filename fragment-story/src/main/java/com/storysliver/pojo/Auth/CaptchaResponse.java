package com.storysliver.pojo.Auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码响应体：返回给前端的数据结构。
 * 干什么用：前端拿 captchaKey 和验证码图片，注册时把 key + 用户输入的答案一起提交。
 * 为什么这样设计：图片只给人看，真正校验靠 key 对应的答案——key 相当于「这次验证码的取件码」。
 * 为什么加 @Data @AllArgsConstructor @NoArgsConstructor：和项目里其他实体保持统一（sims 风格），
 * 无参构造给框架（如 Jackson）留余地，全参构造方便一行 new 出来。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CaptchaResponse {
    private String captchaKey;//本次验证码的标识：注册时带回，服务端凭它找到正确答案
    private String imageBase64;//验证码图片的 base64 字符串：前端直接放进 <img src> 显示，不用再发一次图片请求
}
