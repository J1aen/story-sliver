package com.storysliver.common;

import com.storysliver.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    /** 兜底异常：任何没预料到的错误统一返回 500，不把堆栈细节泄露给前端 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(ResultCode.INTERNAL_ERROR.getCode(), ResultCode.INTERNAL_ERROR.getMessage()));
    }

    /** 错误码 → HTTP 状态码映射，让前端能靠 HTTP status 区分 401/403/404 */
    private HttpStatus statusOf(ResultCode resultCode) {
        return switch (resultCode) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
