package com.storysliver.auth;

import com.storysliver.pojo.Auth.CaptchaResponse;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 算术验证码服务：防止注册被脚本批量刷。
 * 干什么用：生成「a + b = ?」的图片和正确答案；校验用户提交的答案是否正确（一次性）。
 * 为什么用算术题而不是字符验证码：纯 Java 就能画，不依赖第三方图片库；对真人几乎零门槛，对脚本有一定拦截效果。
 * 为什么答案存内存 Map：注册量小、单实例部署，内存够用且零依赖；以后多实例再换 Redis。
 * 为什么标 @Service：它是 Spring 管理的 Bean，Controller 可以直接注入使用。
 */
@Service
public class CaptchaService {

    // key -> 正确答案 的存储
    // 为什么用 ConcurrentHashMap：验证码接口可能被并发请求，ConcurrentHashMap 线程安全，不会丢数据
    private final Map<String, String> answers = new ConcurrentHashMap<>();

    /**
     * 生成一道新的验证码。
     * @return CaptchaResponse（captchaKey + 图片 base64）
     * 逻辑：随机两个 1~9 的整数 → 算出答案存进 Map → 把算式画成图片返回
     */
    public CaptchaResponse generate() {
        // 随机两个 1~9 的整数：答案范围 2~18，简单到真人一眼能算出来
        int a = ThreadLocalRandom.current().nextInt(1, 10);
        int b = ThreadLocalRandom.current().nextInt(1, 10);

        // 生成一个唯一的 key：UUID 几乎不会重复，用它当「这次验证码的取件码」
        String key = UUID.randomUUID().toString();

        // 把正确答案存起来：key -> 答案字符串
        // 为什么存字符串：前端提交的也是字符串，比较时不用类型转换，少踩坑
        answers.put(key, String.valueOf(a + b));

        // 把算式画成图片，连同 key 一起返回给前端
        return new CaptchaResponse(key, renderImage(a + " + " + b + " = ?"));
    }

    /**
     * 校验用户提交的答案。
     * @param key 前端回传的验证码 key
     * @param answer 用户输入的答案
     * @return true=正确 false=错误
     * 为什么用 remove 而不是 get：取走即删除，保证「一次性」——同一个 key 不能被反复试答案
     */
    public boolean verify(String key, String answer) {
        // 从 Map 里取出并删除该 key 的答案；取不到说明 key 不存在或已用过，返回 null
        String expected = answers.remove(key);

        // 判空 + 去空格比较：expected 为 null 直接失败；answer 去掉首尾空格再比，防止用户多打空格
        return expected != null && expected.equals(answer == null ? "" : answer.trim());
    }

    /**
     * 把算式文字画成一张 PNG 图片，转成 base64 字符串。
     * @param text 要画的内容，例如 "3 + 5 = ?"
     * @return data:image/png;base64,xxxxx —— 前端可直接放进 <img src>
     * 为什么用 AWT 画图：JDK 自带，不用引入额外依赖；纯内存生成，不落盘
     */
    private String renderImage(String text) {
        try {
            // 创建一张 140x44 的内存画布，TYPE_INT_RGB 表示每个像素用 RGB 三通道
            BufferedImage image = new BufferedImage(140, 44, BufferedImage.TYPE_INT_RGB);

            // 拿到画笔对象，后面所有绘制都通过它
            Graphics2D g = image.createGraphics();

            // 先铺一层米白底色（247,243,234）：和前端「温暖文字流」风格一致
            g.setColor(new Color(247, 243, 234));
            g.fillRect(0, 0, 140, 44);

            // 用深棕色画算式文字（74,66,56）：和页面文字颜色一致，读起来舒服
            g.setColor(new Color(74, 66, 56));
            // 无衬线字体、加粗、20 号：清楚易读
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
            // 从坐标 (12,30) 开始画文字
            g.drawString(text, 12, 30);

            // 画 6 条浅色干扰线：增加机器识别难度，但不影响真人阅读
            g.setColor(new Color(200, 180, 150));
            for (int i = 0; i < 6; i++) {
                g.drawLine(0, i * 8, 140, i * 8 + 5);// 每条线从左边画到右边，位置错开
            }

            g.dispose();// 释放画笔资源（用完必须释放，否则占内存）

            // 把图片压缩成 PNG 字节流（存在内存里，不写文件）
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);

            // 转 base64 并加 data:image/png;base64, 前缀：前端 <img> 直接显示，省一次图片请求
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            // 图片写入内存流基本不会失败，但按 Java 语法要求必须处理；真失败就抛运行时异常，交给全局异常处理兜底
            throw new IllegalStateException("验证码生成失败", e);
        }
    }
}
