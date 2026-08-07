package com.storysliver.auth;

import com.storysliver.pojo.Auth.CaptchaResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证码服务单元测试。
 * 干什么用：把验证码服务的关键行为写成自动化检查，每次 mvn test 都跑一遍，防止以后改代码改坏功能。
 */
class CaptchaServiceTest {

    // 每个测试方法都 new 一个新的服务实例：测试之间互不干扰、不共享状态
    private final CaptchaService service = new CaptchaService();

    /** 1. generate() 必须能返回 key 和图片 */
    @Test
    void generateReturnsKeyAndImage() {
        CaptchaResponse resp = service.generate();// 生成一个验证码

        // key 不能为空：没有 key 就没法在注册时回传
        assertNotNull(resp.getCaptchaKey());
        // 图片必须以 data:image/png;base64, 开头：前端 <img> 才能直接显示
        assertTrue(resp.getImageBase64().startsWith("data:image/png;base64,"));
    }

    /** 2. 正确答案能通过，且用完后同一个 key 立即失效（一次性） */
    @Test
    void correctAnswerPassesAndKeyConsumedOnce() throws Exception {
        CaptchaResponse resp = service.generate();// 先生成一个验证码

        // 白盒测试：从私有 Map 里取出正确答案（生产环境正常流程是用户看图片自己算答案）
        Field field = CaptchaService.class.getDeclaredField("answers");
        field.setAccessible(true);// 私有字段要放开访问权限
        @SuppressWarnings("unchecked")
        Map<String, String> answers = (Map<String, String>) field.get(service);
        String answer = answers.get(resp.getCaptchaKey());
        assertNotNull(answer);// 答案必须存在：generate 时已经存进去了

        // 正确答案必须校验通过
        assertTrue(service.verify(resp.getCaptchaKey(), answer));
        // 同一个 key 再用任何答案都必须失败：第一次 verify 已经把答案删掉了（一次性）
        assertFalse(service.verify(resp.getCaptchaKey(), answer));
    }

    /** 3. 错误答案会被拒绝，且用错答案同样会消耗掉这个 key（防反复试） */
    @Test
    void wrongAnswerFails() {
        CaptchaResponse resp = service.generate();// 先生成一个

        // 传一个不可能对的答案 999，必须返回 false
        assertFalse(service.verify(resp.getCaptchaKey(), "999"));
        // 同一个 key 再试一次依然 false：第一次已经把答案删掉了
        assertFalse(service.verify(resp.getCaptchaKey(), "999"));
    }

    /** 4. 不存在的 key 直接返回 false */
    @Test
    void unknownKeyReturnsFalse() {
        assertFalse(service.verify("no-such-key", "1"));
    }
}
