package com.myproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproject.entity.dto.Account;
import com.myproject.entity.vo.request.*;
import com.myproject.entity.vo.response.AdminUserVO;
import com.myproject.entity.vo.response.PageVO;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface AccountService extends IService<Account>, UserDetailsService {

        Account findAccountByNameOrEmail(String text);

        String registerEmailVerifyCode(String type, String email, String address);

        String registerEmailAccount(EmailRegisterVO info);

        String resetEmailAccountPassword(EmailResetVO info);

        String resetConfirm(ConfirmResetVO info);

        Account findAccountById(int userId);

        String modifyEmail(int id, ModifyEmailVO vo);

        String ChangePassword(int id, ChangePasswordVO vo);

        // ==================== 管理端 ====================

        PageVO<AdminUserVO> listUsers(int page, int size, String keyword);

        String saveUser(int operatorId, int id, String role, boolean mute, boolean banned);

        String changeUserPassword(int id, String newPassword);
}
