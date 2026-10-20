package com.myproject.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.myproject.entity.BaseData;
import lombok.Data;

import java.util.Date;

@Data
@TableName("db_notice")
public class Notice implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    String title;
    String category;
    String content;
    Integer top;
    String publisher;
    @TableField("publish_time")
    Date publishTime;
    @TableField("update_time")
    Date updateTime;
}
