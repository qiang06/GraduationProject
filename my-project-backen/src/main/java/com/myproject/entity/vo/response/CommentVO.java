package com.myproject.entity.vo.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class CommentVO {
    Integer id;
    String content;
    Integer uid;
    String username;
    String avatar;
    Date time;
    Integer quote;
    Integer rootId;
    /** 被回复人的显示名，前端用于「回复 @xxx」；顶层评论为 null */
    String replyToUsername;
    List<CommentVO> replies;
}
