package com.myproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproject.entity.dto.Account;
import com.myproject.entity.vo.request.*;
import jakarta.validation.Valid;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface AccountService extends IService<Account>, UserDetailsService {

        Account findAccountByNameOrEmail(String text);

        String registerEmailVerifyCode(String type, String email, String address);

        String registerEmailAccount(EmailRegisterVO info);

        String resetEmailAccountPassword(EmailResetVO info);

        String resetConfirm(ConfirmResetVO info);

        Account findAccountById(int userId);

        Object modifyEmail(int id, @Valid ModifyEmailVO vo);

        String ChangePassword(int id, @Valid ChangePasswordVO vo);
}
