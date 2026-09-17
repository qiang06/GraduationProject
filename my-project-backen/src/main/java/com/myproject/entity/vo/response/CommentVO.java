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
    List<CommentVO> replies;
}
