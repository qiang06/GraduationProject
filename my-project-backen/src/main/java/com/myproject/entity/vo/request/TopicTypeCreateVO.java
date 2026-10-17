package com.myproject.entity.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class TopicTypeCreateVO {
    /** 稳定标识，前端路由/后端策略都用它，创建后不再修改 */
    @NotBlank
    @Pattern(regexp = "^[a-z0-9_]{2,32}$", message = "code 只能是小写字母、数字、下划线")
    String code;

    @NotBlank
    @Length(max = 20)
    String name;

    @Length(max = 255)
    String desc;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "颜色必须是 #RRGGBB")
    String color;

    Boolean anonymous;
}
