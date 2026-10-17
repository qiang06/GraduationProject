package com.myproject.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.myproject.entity.BaseData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@TableName("db_topic_comment")
@AllArgsConstructor
@NoArgsConstructor
public class Comment implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    @TableField("uid")
    Integer uid;
    @TableField("tid")
    Integer tid;
    String content;
    Date time;
    @TableField("quote")
    Integer quote;
    /** 顶层评论 id：顶层评论等于自身 id，回复继承父评论的 rootId */
    @TableField("root_id")
    Integer rootId;
}
