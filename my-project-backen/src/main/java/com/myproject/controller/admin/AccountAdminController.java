package com.myproject.controller.admin;

import com.myproject.entity.RestBean;
import com.myproject.entity.vo.response.AdminUserVO;
import com.myproject.entity.vo.response.PageVO;
import com.myproject.service.AccountService;
import com.myproject.service.NotificationService;
import com.myproject.utils.Const;
import com.myproject.utils.ControllerUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/user")
public class AccountAdminController {

    @Resource
    AccountService service;

    @Resource
    ControllerUtils utils;

    @Resource
    NotificationService notificationService;

    @GetMapping("/list")
    public RestBean<PageVO<AdminUserVO>> list(@RequestParam(defaultValue = "1") @Min(1) int page,
                                               @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
                                               @RequestParam(required = false) String keyword) {
        return RestBean.success(service.listUsers(page, size, keyword));
    }

    @PostMapping("/save")
    public RestBean<Void> save(@RequestBody @Valid SaveRequest body,
                               @RequestAttribute(Const.ATTR_USER_ID) int operatorId) {
        String error = service.saveUser(operatorId, body.id(), body.role(),
                Boolean.TRUE.equals(body.mute()), Boolean.TRUE.equals(body.banned()));
        if (error != null) {
            return RestBean.failure(400, error);
        }
        // 给被处理的用户留一条站内消息，他打开消息列表就能看到
        if (Boolean.TRUE.equals(body.mute())) {
            notificationService.create(body.id(), "账号被禁言",
                    "您已被管理员禁言，暂时不能发帖和评论", "system", null);
        }
        if (Boolean.TRUE.equals(body.banned())) {
            notificationService.create(body.id(), "账号被封禁",
                    "您已被管理员封禁，请联系管理员", "system", null);
        }
        return RestBean.success();
    }

    @PostMapping("/change-password")
    public RestBean<Void> changePassword(@RequestBody @Valid PasswordRequest body) {
        return utils.messageHandle(() -> service.changeUserPassword(body.id(), body.newPassword()));
    }

    public record SaveRequest(@NotNull @Min(1) Integer id,
                              @NotNull @Pattern(regexp = "(user|admin)") String role,
                              @NotNull Boolean mute,
                              @NotNull Boolean banned) {
    }

    public record PasswordRequest(@NotNull @Min(1) Integer id,
                                  @NotBlank @Length(min = 6, max = 20) String newPassword) {
    }
}
