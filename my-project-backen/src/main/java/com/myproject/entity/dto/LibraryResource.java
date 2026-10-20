package com.myproject.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.myproject.entity.BaseData;
import lombok.Data;

import java.util.Date;

@Data
@TableName("db_library_resource")
public class LibraryResource implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    String title;
    String author;
    String category;
    String cover;
    String description;
    String url;
    @TableField("create_time")
    Date createTime;
    @TableField("update_time")
    Date updateTime;
}
