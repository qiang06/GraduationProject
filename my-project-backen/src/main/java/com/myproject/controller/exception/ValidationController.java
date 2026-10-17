package com.myproject.controller.exception;

import com.myproject.entity.RestBean;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 接口参数校验的统一处理。
 *
 * 这里刻意不再返回笼统的“请求参数有误”：校验失败时用户最需要知道的是
 * “哪个字段、为什么、边界是多少”，否则只能靠猜。
 */
@Slf4j
@RestControllerAdvice
public class ValidationController {

    /** 字段名 -> 界面上的中文名，缺失时回退为原字段名 */
    private static final Map<String, String> FIELD_LABELS = Map.ofEntries(
            Map.entry("title", "标题"),
            Map.entry("content", "内容"),
            Map.entry("type", "分类"),
            Map.entry("id", "编号"),
            Map.entry("tid", "帖子编号"),
            Map.entry("quote", "引用的评论"),
            Map.entry("page", "页码"),
            Map.entry("size", "每页数量"),
            Map.entry("keyword", "关键词"),
            Map.entry("username", "用户名"),
            Map.entry("password", "密码"),
            Map.entry("old_password", "原密码"),
            Map.entry("new_password", "新密码"),
            Map.entry("email", "邮箱"),
            Map.entry("code", "验证码"),
            Map.entry("status", "状态"),
            Map.entry("phone", "手机号"),
            Map.entry("qq", "QQ 号"),
            Map.entry("wx", "微信号"),
            Map.entry("gender", "性别"),
            Map.entry("sex", "性别"),
            Map.entry("age", "年龄"),
            Map.entry("desc", "简介"),
            Map.entry("file", "文件"),
            Map.entry("studentId", "学号")
    );

    /** 从校验器的默认消息里抽出边界数字，用来自造更直白的中文提示 */
    private static final Pattern NUMBERS = Pattern.compile("\\d+");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public RestBean<Void> handleBodyValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(this::describeFieldError)
                .distinct()
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败: {}", message);
        return RestBean.failure(400, message.isBlank() ? "请求参数有误" : message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public RestBean<Void> handleParamValidation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(this::describeViolation)
                .distinct()
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败: {}", message);
        return RestBean.failure(400, message.isBlank() ? "请求参数有误" : message);
    }

    @ExceptionHandler(ValidationException.class)
    public RestBean<Void> handleValidation(ValidationException exception) {
        log.warn("校验失败: {}", exception.getMessage());
        return RestBean.failure(400, "请求参数有误");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public RestBean<Void> handleUnreadable(HttpMessageNotReadableException exception) {
        log.warn("请求体无法解析: {}", exception.getMessage());
        return RestBean.failure(400, "请求体格式错误，无法解析");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public RestBean<Void> handleIllegalArgument(IllegalArgumentException exception) {
        log.warn("非法参数: {}", exception.getMessage());
        return RestBean.failure(400,
                exception.getMessage() == null ? "请求参数有误" : exception.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public RestBean<Void> runtimeError(RuntimeException exception) {
        log.error("Unhandled request exception", exception);
        return RestBean.failure(500, "服务器内部错误");
    }

    private String describeFieldError(FieldError error) {
        // 带上实际长度，用户才知道该删多少
        Object rejected = error.getRejectedValue();
        Integer actualLength = rejected instanceof CharSequence text ? text.length() : null;
        return this.build(error.getField(), error.getCode(), error.getDefaultMessage(), actualLength);
    }

    private String describeViolation(ConstraintViolation<?> violation) {
        String path = String.valueOf(violation.getPropertyPath());
        int dot = path.lastIndexOf('.');
        String field = dot >= 0 ? path.substring(dot + 1) : path;
        String code = violation.getConstraintDescriptor().getAnnotation()
                .annotationType().getSimpleName();
        Integer actualLength = violation.getInvalidValue() instanceof CharSequence text
                ? text.length() : null;
        return this.build(field, code, violation.getMessage(), actualLength);
    }

    private String build(String field, String code, String defaultMessage, Integer actualLength) {
        String label = FIELD_LABELS.getOrDefault(field == null ? "" : field, field);
        // Spring 给出的错误码是 "Length.createTopicVO.content" 这类复合串，
        // 只取第一段才能匹配到约束名，否则永远落到兜底分支
        String kind = code == null ? "" : code;
        int dot = kind.indexOf('.');
        if (dot > 0) {
            kind = kind.substring(0, dot);
        }
        return switch (kind) {
            case "NotBlank", "NotEmpty", "NotNull" -> "「" + label + "」不能为空";
            case "Length", "Size" -> {
                int[] range = this.numbers(defaultMessage);
                if (range.length < 2) {
                    yield "「" + label + "」长度不符合要求（" + defaultMessage + "）";
                }
                String suffix = actualLength == null ? "" : "，当前 " + actualLength + " 字符";
                String advice = actualLength != null && actualLength > range[1]
                        ? "，请删减约 " + (actualLength - range[1]) + " 字符" : "";
                yield "「" + label + "」长度需在 " + range[0] + " 到 " + range[1] + " 之间"
                        + suffix + advice;
            }
            case "Min", "DecimalMin" -> {
                int[] range = this.numbers(defaultMessage);
                yield range.length >= 1 ? "「" + label + "」不能小于 " + range[0] : "「" + label + "」取值过小";
            }
            case "Max", "DecimalMax" -> {
                int[] range = this.numbers(defaultMessage);
                yield range.length >= 1 ? "「" + label + "」不能大于 " + range[0] : "「" + label + "」取值过大";
            }
            case "Pattern" -> "「" + label + "」包含不允许的字符";
            case "Email" -> "「" + label + "」格式不正确";
            default -> "「" + label + "」" + (defaultMessage == null ? "不符合要求" : defaultMessage);
        };
    }

    private int[] numbers(String text) {
        if (text == null) {
            return new int[0];
        }
        Matcher matcher = NUMBERS.matcher(text);
        List<Integer> found = new ArrayList<>();
        while (matcher.find() && found.size() < 2) {
            found.add(Integer.parseInt(matcher.group()));
        }
        return found.stream().mapToInt(Integer::intValue).toArray();
    }
}
