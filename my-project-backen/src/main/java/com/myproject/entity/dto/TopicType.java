package com.myproject.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.myproject.entity.BaseData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@TableName("db_topic_type")
@AllArgsConstructor
@NoArgsConstructor
public class TopicType implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    String name;
    @TableField("`desc`")
    String desc;
    String color;
    /** 稳定的业务标识：lost / activity / confession / chat ...，路由与匿名判断都用它 */
    String code;
    /** 该分类是否匿名展示（表白墙为 true） */
    Boolean anonymous;
}
