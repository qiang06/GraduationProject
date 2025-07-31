package com.myproject.controller;

import com.myproject.entity.RestBean;
import com.myproject.entity.dto.Account;
import com.myproject.entity.dto.AccountDetails;
import com.myproject.entity.vo.request.ChangePasswordVO;
import com.myproject.entity.vo.request.DetailsSaveVO;
import com.myproject.entity.vo.request.ModifyEmailVO;
import com.myproject.entity.vo.request.PrivacySaveVO;
import com.myproject.entity.vo.response.AccountDetailsVO;
import com.myproject.entity.vo.response.AccountPrivacyVO;
import com.myproject.entity.vo.response.AccountVO;
import com.myproject.service.AccountDetailsService;
import com.myproject.service.AccountPrivacyService;
import com.myproject.service.AccountService;
import com.myproject.utils.Const;
import com.myproject.utils.ControllerUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class AccountController {

    @Resource
    private AccountService accountService;
    @Resource
    private AccountDetailsService accountDetailsService;
    @Resource
    private ControllerUtils utils;
    @Resource
    private AccountPrivacyService privacyService;

    @GetMapping("/info")
    public RestBean<AccountVO> info(@RequestAttribute(Const.ATTR_USER_ID) int userId) {
        Account account = accountService.findAccountById(userId);
        return RestBean.success(account.asViewObject(AccountVO.class));
    }

    @GetMapping("/details")
    public RestBean<AccountDetailsVO> details(@RequestAttribute(Const.ATTR_USER_ID) int userId) {
        /*
        1. **`accountDetailsService.findAccountDetailsById(userId)`**:
    - 调用 接口的 方法，传入 参数。 `AccountDetailsService``findAccountDetailsById``userId`
    - 这个方法会尝试从数据库中查找与给定 相关的账户详细信息。 `userId`

2. **`Optional.ofNullable(...)`**:
    - 方法用于将可能为 `null` 的结果包装成一个 对象。 `Optional.ofNullable``Optional`
    - 如果 方法返回的结果不为 `null`，则 对象将包含这个结果；否则，它将是一个空的 对象。 `findAccountDetailsById``Optional``Optional`

3. **`orElseGet(AccountDetails::new)`**:
    - 方法用于在 对象为空时提供一个默认值。 `orElseGet``Optional`
    - 在这里，如果 对象为空（即 返回 `null`），则调用 创建一个新的 对象。 `Optional``findAccountDetailsById``AccountDetails::new``AccountDetails`
    - 是一个方法引用，表示调用 类的无参构造函数来创建一个新的对象。 `AccountDetails::new``AccountDetails`

         */
        AccountDetails details = Optional
                .ofNullable(accountDetailsService.findAccountDetailsById(userId))
                .orElseGet(AccountDetails::new);
        return RestBean.success(details.asViewObject(AccountDetailsVO.class));
    }

    @PostMapping("/save-details")
    public RestBean<Void> saveDetails(@RequestAttribute(Const.ATTR_USER_ID) int userId,
                                      @RequestBody @Valid DetailsSaveVO vo) {

        boolean success = accountDetailsService.saveAccountDetails(userId, vo);
        return success ? RestBean.success() : RestBean.failure(400, Const.USERNAME_HAS_BEEN_USED);
    }

    @PostMapping("/modify-email")
    public RestBean<Void> modifyEmail(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                      @RequestBody @Valid ModifyEmailVO vo) {


        return utils.messageHandle(() -> accountService.modifyEmail(id, vo).toString());

    }

    @PostMapping("/change-password")
    public RestBean<Void> changePassword(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                         @RequestBody @Valid ChangePasswordVO vo) {

        return utils.messageHandle(() -> accountService.ChangePassword(id, vo));

    }

    @PostMapping("/save-privacy")
    public RestBean<Void> savePrivacy(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                         @RequestBody @Valid PrivacySaveVO vo) {

        privacyService.savePrivacy(id, vo);
        return RestBean.success();

    }
    @GetMapping("/privacy")
    public RestBean<AccountPrivacyVO> Privacy(@RequestAttribute(Const.ATTR_USER_ID) int id) {

    return RestBean.success(privacyService.accountPrivacy(id).asViewObject(AccountPrivacyVO.class));

    }

}
