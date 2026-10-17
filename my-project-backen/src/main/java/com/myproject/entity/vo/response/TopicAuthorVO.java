package com.myproject.entity.vo.response;

import lombok.Data;

/**
 * 帖子作者信息，按 db_account_privacy 过滤后再下发。
 * 为 null 的字段表示「对方设置为隐藏」或「未填写」，前端统一显示为未填写。
 */
@Data
public class TopicAuthorVO {
    Integer id;
    String username;
    String avatar;
    String email;
    Integer gender;
    String phone;
    String qq;
    String wx;
    String desc;
}
