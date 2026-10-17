package com.myproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myproject.entity.dto.Notification;
import com.myproject.entity.vo.response.NotificationVO;
import com.myproject.entity.vo.response.PageVO;
import com.myproject.mapper.NotificationMapper;
import com.myproject.service.NotificationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {
    @Resource NotificationMapper mapper;

    @Override
    public void create(int uid, String title, String content, String type, String url) {
        mapper.insert(new Notification(null, uid, title, content, type, url, new Date(), 0));
    }

    @Override
    public PageVO<NotificationVO> list(int uid, int page, int size) {
        Page<Notification> result = mapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUid, uid).orderByDesc(Notification::getTime));
        List<NotificationVO> items = result.getRecords().stream().map(n -> {
            NotificationVO vo = n.asViewObject(NotificationVO.class);
            // 实体字段叫 isRead（避开 MySQL 保留字 read），前端仍用 read
            vo.setRead(n.getIsRead());
            return vo;
        }).toList();
        return PageVO.of(items, result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    @Override public long unreadCount(int uid) {
        return mapper.selectCount(new LambdaQueryWrapper<Notification>().eq(Notification::getUid, uid).eq(Notification::getIsRead, 0));
    }

    @Override public boolean markRead(int uid, int id) {
        return mapper.update(null, new LambdaUpdateWrapper<Notification>().eq(Notification::getId, id).eq(Notification::getUid, uid).eq(Notification::getIsRead, 0).set(Notification::getIsRead, 1)) > 0;
    }

    @Override public int markAllRead(int uid) {
        return mapper.update(null, new LambdaUpdateWrapper<Notification>().eq(Notification::getUid, uid).eq(Notification::getIsRead, 0).set(Notification::getIsRead, 1));
    }
}
