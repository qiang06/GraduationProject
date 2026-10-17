package com.myproject.controller.admin;

import com.myproject.entity.RestBean;
import com.myproject.entity.dto.Topic;
import com.myproject.entity.vo.request.TopicTypeCreateVO;
import com.myproject.entity.vo.request.TopicTypeUpdateVO;
import com.myproject.entity.vo.response.PageVO;
import com.myproject.entity.vo.response.TopicPreviewVO;
import com.myproject.service.NotificationService;
import com.myproject.service.TopicPostService;
import com.myproject.utils.ControllerUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/forum")
public class ForumAdminController {

    @Resource
    TopicPostService service;

    @Resource
    ControllerUtils utils;

    @Resource
    NotificationService notificationService;

    @GetMapping("/list")
    public RestBean<PageVO<TopicPreviewVO>> list(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                  @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
                                                  @RequestParam(required = false) String keyword) {
        return RestBean.success(service.listAllTopics(page, size, keyword));
    }

    @PostMapping("/delete")
    public RestBean<Void> delete(@RequestBody @Valid IdRequest body) {
        service.adminDeleteTopic(body.id());
        return RestBean.success();
    }

    @PostMapping("/top")
    public RestBean<Void> top(@RequestBody @Valid StatusRequest body) {
        boolean enabled = Boolean.TRUE.equals(body.status());
        service.setTopicTop(body.tid(), enabled);
        if (enabled) {
            notifyTopic(body.tid(), "帖子被置顶", "你的帖子已被管理员置顶");
        }
        return RestBean.success();
    }

    @PostMapping("/locked")
    public RestBean<Void> locked(@RequestBody @Valid StatusRequest body) {
        boolean enabled = Boolean.TRUE.equals(body.status());
        service.setTopicLocked(body.tid(), enabled);
        if (enabled) {
            notifyTopic(body.tid(), "帖子被锁定", "你的帖子已被管理员锁定，暂时不能评论和编辑");
        }
        return RestBean.success();
    }

    @PostMapping("/invisible")
    public RestBean<Void> invisible(@RequestBody @Valid StatusRequest body) {
        boolean enabled = Boolean.TRUE.equals(body.status());
        service.setTopicInvisible(body.tid(), enabled);
        if (enabled) {
            notifyTopic(body.tid(), "帖子被屏蔽", "你的帖子已被管理员屏蔽，普通用户暂时看不到");
        }
        return RestBean.success();
    }

    /** 管理端操作后给帖子作者留一条站内消息；屏蔽/锁定/置顶都只在实际开启时通知 */
    private void notifyTopic(int tid, String title, String content) {
        Topic topic = service.getById(tid);
        if (topic != null && topic.getUid() != null) {
            notificationService.create(topic.getUid(), title, content, "system", "/index/topic/" + tid);
        }
    }

    @PostMapping("/change-type")
    public RestBean<Void> changeType(@RequestBody @Valid ChangeTypeRequest body) {
        return utils.messageHandle(() -> service.changeTopicType(body.tid(), body.type()));
    }

    @PostMapping("/type/create")
    public RestBean<Void> createType(@RequestBody @Valid TopicTypeCreateVO vo) {
        return utils.messageHandle(() -> service.createTopicType(vo));
    }

    @PostMapping("/type/update")
    public RestBean<Void> updateType(@RequestBody @Valid TopicTypeUpdateVO vo) {
        return utils.messageHandle(() -> service.updateTopicType(vo));
    }

    @PostMapping("/type/delete")
    public RestBean<Void> deleteType(@RequestBody @Valid IdRequest body) {
        return utils.messageHandle(() -> service.deleteTopicType(body.id()));
    }

    public record IdRequest(@NotNull @Min(1) Integer id) {
    }

    public record StatusRequest(@NotNull @Min(1) Integer tid,
                                @NotNull Boolean status) {
    }

    public record ChangeTypeRequest(@NotNull @Min(1) Integer tid,
                                    @NotNull @Min(1) Integer type) {
    }
}
