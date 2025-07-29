package com.myproject.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproject.entity.dto.Account;
import com.myproject.entity.dto.AccountDetails;
import com.myproject.entity.vo.request.DetailsSaveVO;
import com.myproject.mapper.AccountDetailsMapper;
import com.myproject.service.AccountDetailsService;
import com.myproject.service.AccountService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDetailsServiceImpl extends ServiceImpl<AccountDetailsMapper, AccountDetails> implements AccountDetailsService {


    @Resource
    AccountService accountService;

    @Override
    public AccountDetails findAccountDetailsById(int id) {

        return this.getById(id);
    }

    @Override
    @Transactional
    public boolean saveAccountDetails(int id, DetailsSaveVO vo) {
        Account account = accountService.findAccountByNameOrEmail(vo.getUsername());
        if (account == null || account.getId() == id) {
            accountService.update()
                    .eq("id",id)
                    .set("username",vo.getUsername())
                    .update();

            this.saveOrUpdate(new AccountDetails(
                    id,vo.getGender(),vo.getPhone(),
                    vo.getQq(),vo.getWx(),vo.getDesc()
            ));
            return true;
        }
        return false;
    }
}
