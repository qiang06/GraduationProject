package com.myproject.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.myproject.entity.BaseData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@TableName("db_topic")
@AllArgsConstructor
@NoArgsConstructor
public class Topic implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    String title;
    String content;
    Integer uid;
    Integer type;
    Date time;
    Integer top;
}
