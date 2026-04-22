package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface UserAccountService extends IService<UserAccount> {


    void initUserAccount(Map<String, Object> msg);

    void saveUserAccountDetail(UserAccountDetail userAccountDetail);
}
