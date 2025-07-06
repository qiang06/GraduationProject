package com.myproject.controller;


import com.myproject.entity.RestBean;
import com.myproject.entity.vo.request.ConfirmResetVO;
import com.myproject.entity.vo.request.EmailRegisterVO;
import com.myproject.entity.vo.request.EmailResetVO;
import com.myproject.service.AccountService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 用于验证相关Controller包含用户的注册、重置密码等操作
 */
@Validated
@RestController
@RequestMapping("/api/auth")
//@Tag(name = "登录校验相关", description = "包括用户登录、注册、验证码请求等操作。")
public class AuthorizeController {

    @Resource
    AccountService accountService;

    /**
     * 请求邮件验证码
     * @param email 请求邮件
     * @param type 类型
     * @param request 请求
     * @return 是否请求成功
     */
    @GetMapping("/ask-code")
//    @Operation(summary = "请求邮件验证码")
    public RestBean<Void> askVerifyCode(@RequestParam  @Email String email,
                                        @RequestParam  @Pattern(regexp = "(register|reset)") String type,
                                        HttpServletRequest request) {
//        String message = accountService.registerEmailVerifyCode(type, email, request.getRemoteAddr());

        return this.messageHandle(() -> accountService.registerEmailVerifyCode(type, email, request.getRemoteAddr()));
    }
    @PostMapping("/register")
    public RestBean<Void> register(@RequestBody @Validated EmailRegisterVO vo) {
        return this.messageHandle(vo,accountService::registerEmailAccount);
    }

    /**
     * 执行密码重置确认，检查验证码是否正确
     * @param vo 密码重置信息
     * @return 是否操作成功
     */
    @PostMapping("/reset-confirm")
    public RestBean<Void> resetConfirm(@RequestBody @Valid ConfirmResetVO vo){
        return this.messageHandle(vo,accountService::resetConfirm);
    }

    @PostMapping("/reset-password")
    public RestBean<Void> resetConfirm(@RequestBody @Valid EmailResetVO vo){
        return this.messageHandle(vo,accountService::resetEmailAccountPassword);
    }

    private <T>RestBean<Void> messageHandle(T vo, Function<T,String> function) {
        return messageHandle(()-> function.apply(vo));
    }

    private RestBean<Void> messageHandle(Supplier<String> action) {
        String message = action.get();
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }

}
