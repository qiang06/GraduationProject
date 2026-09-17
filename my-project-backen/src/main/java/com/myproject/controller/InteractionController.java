package com.myproject.controller;

import com.myproject.entity.RestBean;
import com.myproject.entity.vo.response.TopicInteractionVO;
import com.myproject.service.InteractionService;
import com.myproject.utils.Const;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/forum/interaction")
public class InteractionController {
    @Resource InteractionService service;

    @GetMapping
    public RestBean<TopicInteractionVO> interaction(@RequestParam @Min(1) int tid,
                                                     @RequestAttribute(Const.ATTR_USER_ID) int uid) {
        TopicInteractionVO vo = service.getTopicInteraction(tid, uid);
        return vo == null ? RestBean.failure(400, "帖子不存在") : RestBean.success(vo);
    }

    @PostMapping("/like")
    public RestBean<TopicInteractionVO> like(@RequestBody @Valid IdRequest request,
                                              @RequestAttribute(Const.ATTR_USER_ID) int uid) {
        return RestBean.success(service.toggleLike(request.id(), uid));
    }

    @PostMapping("/collect")
    public RestBean<TopicInteractionVO> collect(@RequestBody @Valid IdRequest request,
                                                 @RequestAttribute(Const.ATTR_USER_ID) int uid) {
        return RestBean.success(service.toggleCollect(request.id(), uid));
    }

    public record IdRequest(@Min(1) int id) {}
}
