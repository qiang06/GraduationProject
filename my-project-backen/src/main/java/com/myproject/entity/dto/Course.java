package com.myproject.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.myproject.entity.BaseData;
import lombok.Data;

import java.util.Date;

@Data
@TableName("db_course")
public class Course implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer uid;
    String name;
    String teacher;
    String location;
    Integer weekday;
    @TableField("start_section")
    Integer startSection;
    @TableField("end_section")
    Integer endSection;
    String weeks;
    String color;
    String note;
    @TableField("create_time")
    Date createTime;
    @TableField("update_time")
    Date updateTime;
}
