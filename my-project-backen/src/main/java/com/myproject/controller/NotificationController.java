package com.myproject.controller;

import com.myproject.entity.RestBean;
import com.myproject.entity.vo.response.NotificationVO;
import com.myproject.entity.vo.response.PageVO;
import com.myproject.service.NotificationService;
import com.myproject.utils.Const;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/notification")
public class NotificationController {
    @Resource NotificationService service;

    @GetMapping("/list")
    public RestBean<PageVO<NotificationVO>> list(@RequestAttribute(Const.ATTR_USER_ID) int uid,
                                                  @RequestParam(defaultValue="1") @Min(1) int page,
                                                  @RequestParam(defaultValue="10") @Min(1) @Max(50) int size) {
        return RestBean.success(service.list(uid, page, size));
    }

    @GetMapping("/unread")
    public RestBean<Map<String, Long>> unread(@RequestAttribute(Const.ATTR_USER_ID) int uid) {
        return RestBean.success(Map.of("count", service.unreadCount(uid)));
    }

    @PostMapping("/read")
    public RestBean<Void> read(@RequestAttribute(Const.ATTR_USER_ID) int uid, @RequestBody @Valid IdRequest request) {
        service.markRead(uid, request.id());
        return RestBean.success();
    }

    @PostMapping("/read-all")
    public RestBean<Void> readAll(@RequestAttribute(Const.ATTR_USER_ID) int uid) {
        service.markAllRead(uid);
        return RestBean.success();
    }

    public record IdRequest(@Min(1) int id) {}
}
