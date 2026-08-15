package com.storysliver.common;

import com.storysliver.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理：统一把异常转成 Result 返回。
 * 为什么这样设计：业务层只管抛 BusinessException，这里统一兜底，Controller 不用写 try-catch。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：按错误码映射 HTTP 状态码并返回提示信息 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ResponseEntity.status(statusOf(e.getResultCode()))
                .body(Result.error(e.getResultCode().getCode(), e.getMessage()));
    }

    /** 参数校验异常：把每个字段的错误信息拼成一句话返回 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(Result.error(ResultCode.BAD_REQUEST.getCode(), message));
    }

    /** 请求体不是合法 JSON：返回 400，而不是 500 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Result.error(ResultCode.BAD_REQUEST.getCode(), "请求体格式错误"));
    }

    /** 上传文件超限（Spring multipart 在进 Controller 前拦截）：返回 400 友好提示，而不是 500 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.badRequest().body(Result.error(ResultCode.BAD_REQUEST.getCode(), "图片太大，不能超过 5MB"));
    }

    /** 路径不存在（没有对应 Controller）：返回 404 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.error(ResultCode.NOT_FOUND.getCode(), "接口不存在"));
    }

    /** 兜底异常：任何没预料到的错误统一返回 500，不把堆栈细节泄露给前端 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(ResultCode.INTERNAL_ERROR.getCode(), ResultCode.INTERNAL_ERROR.getMessage()));
    }

    /**
     * 错误码 → HTTP 状态码映射。
     * 为什么按 code 数字而不是按枚举名匹配：
     * 业务错误码会不断新增（验证码 400、限流 429、登录失败 401……），
     * 按数字归类后，新增错误码不用再改这里。
     */
    private HttpStatus statusOf(ResultCode resultCode) {
        return switch (resultCode.getCode()) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
