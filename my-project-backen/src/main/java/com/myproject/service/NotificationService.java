package com.myproject.service;

import com.myproject.entity.dto.Notification;
import com.myproject.entity.vo.response.NotificationVO;
import com.myproject.entity.vo.response.PageVO;

public interface NotificationService {
    void create(int uid, String title, String content, String type, String url);
    PageVO<NotificationVO> list(int uid, int page, int size);
    long unreadCount(int uid);
    boolean markRead(int uid, int id);
    int markAllRead(int uid);
}
