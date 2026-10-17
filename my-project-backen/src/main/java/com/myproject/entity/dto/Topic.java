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
    /** 管理端锁定：禁止评论/编辑 */
    Integer locked;
    /** 管理端屏蔽：普通用户列表/详情不可见 */
    Integer invisible;
    /** 类型专属字段的 JSON 文本：失物招领 item/place/status、校园活动时间地点等 */
    String extra;
}
