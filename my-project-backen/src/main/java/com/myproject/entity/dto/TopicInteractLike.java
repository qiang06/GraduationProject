package com.myproject.entity.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_topic_interact_like")
public class TopicInteractLike {
    Integer tid;
    Integer uid;
    Date time;
}
