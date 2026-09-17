package com.myproject.service;

import com.myproject.entity.vo.response.TopicInteractionVO;

public interface InteractionService {
    TopicInteractionVO getTopicInteraction(int tid, int uid);
    TopicInteractionVO toggleLike(int tid, int uid);
    TopicInteractionVO toggleCollect(int tid, int uid);
}
