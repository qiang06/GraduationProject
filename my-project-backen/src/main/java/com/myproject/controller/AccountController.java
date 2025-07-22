package com.myproject.controller;

import com.myproject.entity.RestBean;
import com.myproject.entity.dto.Account;
import com.myproject.entity.vo.response.AccountVO;
import com.myproject.service.AccountService;
import com.myproject.utils.Const;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class AccountController {

    @Resource
    private AccountService accountService;

    @GetMapping("/info")
    public RestBean<AccountVO> info(@RequestAttribute(Const.ATTR_USER_ID) int userId) {
        Account  account = accountService.findAccountById(userId);
        return RestBean.success(account.asViewObject(AccountVO.class));
    }

}
