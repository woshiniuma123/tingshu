package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.Map;

public interface UserAccountService extends IService<UserAccount> {


    void initUserAccount(Map<String, Object> msg);

    void saveUserAccountDetail(UserAccountDetail userAccountDetail);

    /**
     * 获取当前用户的账户可用余额
     *
     * @return
     */
    BigDecimal getAvailableAmount(Long userId);

    void checkAndDeduct(AccountDeductVo accountDeductVo);

}
