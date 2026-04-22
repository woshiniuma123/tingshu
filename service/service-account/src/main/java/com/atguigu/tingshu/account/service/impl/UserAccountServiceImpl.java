package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

    @Autowired
    private UserAccountMapper userAccountMapper;
    @Autowired
    private UserAccountDetailMapper userAccountDetailMapper;

    /**
     * 初始化用户账户信息
     *
     * @param msg
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initUserAccount(Map<String, Object> msg) {
        Long userId = (Long) msg.get("userId");
        Integer amount = (Integer) msg.get("amount");
        BigDecimal bamount = BigDecimal.valueOf(amount);
        String title = (String) msg.get("title");
        String orderNo = (String) msg.get("orderNo");
        //1.新增用户的账户表的信息
        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(userId);
        userAccount.setAvailableAmount(bamount);
        userAccount.setLockAmount(bamount);
        userAccount.setTotalAmount(bamount);
        userAccount.setTotalIncomeAmount(bamount);
        userAccount.setTotalPayAmount(BigDecimal.ZERO);
        userAccountMapper.insert(userAccount);
        //2.添加用户账户日志表信息
        UserAccountDetail userAccountDetail = new UserAccountDetail();
        userAccountDetail.setUserId(userId);
        userAccountDetail.setTitle(title);
        userAccountDetail.setOrderNo(orderNo);
        userAccountDetail.setAmount(bamount);
        userAccountDetail.setTradeType(SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT);
        this.saveUserAccountDetail(userAccountDetail);

    }

    @Override
    public void saveUserAccountDetail(UserAccountDetail userAccountDetail) {
        userAccountDetail.setAmount(userAccountDetail.getAmount());
        userAccountDetail.setOrderNo(userAccountDetail.getOrderNo());
        userAccountDetail.setTitle(userAccountDetail.getTitle());
        userAccountDetail.setUserId(userAccountDetail.getUserId());
        userAccountDetail.setTradeType(SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT);
        userAccountDetailMapper.insert(userAccountDetail);
    }
}
