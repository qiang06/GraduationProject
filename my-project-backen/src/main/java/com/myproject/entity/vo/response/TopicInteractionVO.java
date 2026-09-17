package com.myproject.entity.vo.response;

import lombok.Data;

@Data
public class TopicInteractionVO {
    Integer likeCount;
    Integer collectCount;
    boolean liked;
    boolean collected;
}
