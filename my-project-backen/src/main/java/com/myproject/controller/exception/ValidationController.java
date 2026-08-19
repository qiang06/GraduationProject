package com.myproject.controller.exception;


import com.myproject.entity.RestBean;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 用于接口参数校验处理的控制器
 */
@Slf4j
@RestControllerAdvice
public class ValidationController {

    /**
     * 与SpringBoot保持一致，校验不通过打印警告信息，而不是直接抛出异常
     * @param exception 验证异常
     * @return 校验结果
     */
    @ExceptionHandler({ValidationException.class, MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public RestBean<Void> validateError(Exception exception) {
        log.warn("Resolved [{}: {}]", exception.getClass().getName(), exception.getMessage());
        return RestBean.failure(400, "请求参数有误");
    }

    @ExceptionHandler(RuntimeException.class)
    public RestBean<Void> runtimeError(RuntimeException exception) {
        log.error("Unhandled request exception", exception);
        return RestBean.failure(500, "服务器内部错误");
    }
}
