package com.myproject.entity.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_topic_interact_collect")
public class TopicInteractCollect {
    Integer tid;
    Integer uid;
    Date time;
}
