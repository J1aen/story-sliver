package com.storysliver.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应体：所有接口都返回这个格式。
 * 干什么用：前端统一判断 code 是否 200，取 text 显示提示、data 取数据。
 * 为什么这样设计：照抄你之前 sims 项目的 Result 格式，前后端约定简单统一。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private Integer code;//响应码：200 成功
    private String text;//提示信息
    private Object data;//返回的数据

    /** 增删改成功：不需要返回数据 */
    public static Result success() {
        return new Result(200, "success", null);
    }

    /** 查询成功：带数据返回 */
    public static Result success(Object data) {
        return new Result(200, "success", data);
    }

    /** 失败响应：默认 400 */
    public static Result error(String text) {
        return new Result(400, text, null);
    }

    /** 指定错误码的失败响应（配合 ResultCode 枚举使用） */
    public static Result error(Integer code, String text) {
        return new Result(code, text, null);
    }
}
