package com.myproject;

import com.myproject.controller.exception.ValidationController;
import com.myproject.entity.RestBean;
import com.myproject.entity.vo.request.CreateTopicVO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 校验失败时必须返回“哪个字段、为什么、边界多少”，
 * 否则用户对着“请求参数有误”无从下手。
 */
class ValidationMessageTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ValidationController controller = new ValidationController();

    private String messageFor(Object target, String objectName) {
        Set<ConstraintViolation<Object>> violations = validator.validate(target);
        assertFalse(violations.isEmpty(), "该用例应当产生校验失败");
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(target, objectName);
        for (ConstraintViolation<Object> violation : violations) {
            String field = violation.getPropertyPath().toString();
            // 复刻 Spring 的错误码构造方式：首段是 "约束名.对象名.字段"，
            // 传 null 会让 FieldError.getCode() 返回 null，测出来的行为与线上不符
            String name = violation.getConstraintDescriptor().getAnnotation()
                    .annotationType().getSimpleName();
            String[] codes = {name + "." + objectName + "." + field, name + "." + field, name};
            binding.addError(new FieldError(objectName, field, violation.getInvalidValue(),
                    false, codes, null, violation.getMessage()));
        }
        // MethodParameter 传 null：处理器只用到 bindingResult
        RestBean<Void> bean = controller.handleBodyValidation(
                new MethodArgumentNotValidException(null, binding));
        return bean.message();
    }

    @Test
    void oversizedContentReportsFieldLimitAndHowMuchToCut() {
        CreateTopicVO vo = new CreateTopicVO();
        vo.setTitle("正常标题");
        vo.setType(1);
        vo.setContent("a".repeat(20001));

        String message = messageFor(vo, "createTopicVO");
        System.out.println("超长内容提示 -> " + message);

        assertTrue(message.contains("内容"), "应点名内容字段: " + message);
        assertTrue(message.contains("20000"), "应给出上限: " + message);
        assertTrue(message.contains("20001"), "应给出当前长度: " + message);
        assertTrue(message.contains("删减约 1"), "应告诉用户删多少: " + message);
    }

    @Test
    void blankTitleIsReportedByFieldName() {
        CreateTopicVO vo = new CreateTopicVO();
        vo.setTitle("   ");
        vo.setType(1);
        vo.setContent("<p>正常内容</p>");

        String message = messageFor(vo, "createTopicVO");
        System.out.println("空标题提示 -> " + message);
        assertTrue(message.contains("标题"), message);
    }

    @Test
    void missingTypeIsReportedNotAsGenericError() {
        CreateTopicVO vo = new CreateTopicVO();
        vo.setTitle("正常标题");
        vo.setContent("<p>正常内容</p>");

        String message = messageFor(vo, "createTopicVO");
        System.out.println("缺分类提示 -> " + message);
        assertTrue(message.contains("分类"), message);
        assertNotEquals("请求参数有误", message, "不应退化成笼统提示");
    }
}
