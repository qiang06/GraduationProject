package com.myproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myproject.entity.dto.Topic;
import com.myproject.entity.dto.TopicInteractCollect;
import com.myproject.entity.dto.TopicInteractLike;
import com.myproject.entity.vo.response.TopicInteractionVO;
import com.myproject.mapper.TopicInteractCollectMapper;
import com.myproject.mapper.TopicInteractLikeMapper;
import com.myproject.mapper.TopicMapper;
import com.myproject.service.InteractionService;
import com.myproject.service.NotificationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class InteractionServiceImpl implements InteractionService {
    @Resource TopicMapper topicMapper;
    @Resource TopicInteractLikeMapper likeMapper;
    @Resource TopicInteractCollectMapper collectMapper;
    @Resource NotificationService notificationService;

    @Override
    public TopicInteractionVO getTopicInteraction(int tid, int uid) {
        Topic topic = topicMapper.selectById(tid);
        if (topic == null) return null;
        return snapshot(tid, uid);
    }

    @Override
    public TopicInteractionVO toggleLike(int tid, int uid) {
        ensureTopic(tid);
        LambdaQueryWrapper<TopicInteractLike> q = new LambdaQueryWrapper<TopicInteractLike>()
                .eq(TopicInteractLike::getTid, tid).eq(TopicInteractLike::getUid, uid);
        if (likeMapper.selectCount(q) > 0) {
            likeMapper.delete(q);
        } else {
            likeMapper.insert(new TopicInteractLike(tid, uid, new Date()));
            notifyOwner(tid, uid, "帖子收到新点赞", "有人点赞了你的帖子", "like");
        }
        return snapshot(tid, uid);
    }

    @Override
    public TopicInteractionVO toggleCollect(int tid, int uid) {
        ensureTopic(tid);
        LambdaQueryWrapper<TopicInteractCollect> q = new LambdaQueryWrapper<TopicInteractCollect>()
                .eq(TopicInteractCollect::getTid, tid).eq(TopicInteractCollect::getUid, uid);
        if (collectMapper.selectCount(q) > 0) {
            collectMapper.delete(q);
        } else {
            collectMapper.insert(new TopicInteractCollect(tid, uid, new Date()));
            notifyOwner(tid, uid, "帖子被收藏", "有人收藏了你的帖子", "collect");
        }
        return snapshot(tid, uid);
    }

    private TopicInteractionVO snapshot(int tid, int uid) {
        TopicInteractionVO vo = new TopicInteractionVO();
        vo.setLikeCount(Math.toIntExact(likeMapper.selectCount(new LambdaQueryWrapper<TopicInteractLike>().eq(TopicInteractLike::getTid, tid))));
        vo.setCollectCount(Math.toIntExact(collectMapper.selectCount(new LambdaQueryWrapper<TopicInteractCollect>().eq(TopicInteractCollect::getTid, tid))));
        vo.setLiked(likeMapper.selectCount(new LambdaQueryWrapper<TopicInteractLike>().eq(TopicInteractLike::getTid, tid).eq(TopicInteractLike::getUid, uid)) > 0);
        vo.setCollected(collectMapper.selectCount(new LambdaQueryWrapper<TopicInteractCollect>().eq(TopicInteractCollect::getTid, tid).eq(TopicInteractCollect::getUid, uid)) > 0);
        return vo;
    }

    private void notifyOwner(int tid, int actorUid, String title, String content, String type) {
        Topic topic = topicMapper.selectById(tid);
        if (topic != null && !topic.getUid().equals(actorUid)) {
            notificationService.create(topic.getUid(), title, content, type, "/index/topic/" + tid);
        }
    }

    private void ensureTopic(int tid) {
        if (topicMapper.selectById(tid) == null) throw new IllegalArgumentException("帖子不存在");
    }
}
