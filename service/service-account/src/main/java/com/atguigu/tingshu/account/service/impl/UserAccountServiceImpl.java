package com.atguigu.tingshu.account.service.impl;

import cn.hutool.core.lang.Assert;
import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
        userAccountDetail.setTradeType(userAccountDetail.getTradeType());
        userAccountDetailMapper.insert(userAccountDetail);
    }

    /**
     * 获取当前用户的账户可用余额
     *
     * @return
     */
    @Override
    public BigDecimal getAvailableAmount(Long userId) {
        UserAccount userAccount = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>()
                        .eq(UserAccount::getUserId, userId)
                        .select(UserAccount::getAvailableAmount)
        );
        Assert.notNull(userAccount, "当前用户{}，账户信息为空", userId);
        return userAccount.getAvailableAmount();
    }

    /**
     * 检查并扣减账户余额
     *
     * @param accountDeductVo
     */
    @Override
    public void checkAndDeduct(AccountDeductVo accountDeductVo) {
        BigDecimal amount = accountDeductVo.getAmount();
        Long userId = accountDeductVo.getUserId();
        //1.先检查当前用户的账户余额是否充足
        UserAccount userAccount = userAccountMapper
                .checkAndDeduct(userId, amount);
        if (userAccount == null) {
            //当前用户余额不足
            throw new GuiguException(500, "您当前的账户余额不足");
        }
        //2.余额充足则讲扣减当前用户的账户余额、增加用户的支出金额
        userAccountMapper.update(null,
                new LambdaUpdateWrapper<UserAccount>()
                        .eq(UserAccount::getUserId, userId)
                        .setSql("total_amount = total_amount-" + amount)
                        .setSql("available_amount=available_amount-" + amount)
                        .setSql("total_pay_amount=total_pay_amount+" + amount)
        );
        //3.增加用户的账户明细
        UserAccountDetail userAccountDetail = new UserAccountDetail();
        userAccountDetail.setUserId(userId);
        userAccountDetail.setTradeType(SystemConstant.ACCOUNT_TRADE_TYPE_MINUS);
        userAccountDetail.setTitle(accountDeductVo.getContent());
        userAccountDetail.setOrderNo(accountDeductVo.getOrderNo());
        userAccountDetail.setAmount(amount);
        this.saveUserAccountDetail(userAccountDetail);
    }

    /**
     * 充值成功后更新用户的账户信息
     *
     * @param userId
     */
    @Override
    public void updateUserAccount(Long userId, BigDecimal rechargeAmount) {
        userAccountMapper.update(null,
                new LambdaUpdateWrapper<UserAccount>()
                        .eq(UserAccount::getUserId, userId)
                        .setSql("total_amount=total_amount+" + rechargeAmount)
                        .setSql("available_amount=available_amount+" + rechargeAmount)
                        .setSql("total_income_amount=total_income_amount+" + rechargeAmount)
        );
    }

    /**
     * 获取用户的充值或消费信息
     *
     * @param userId
     * @param page
     * @return
     */
    @Override
    public Page<UserAccountDetail> getUserAccountDetail(Long userId, Page<UserAccountDetail> pageInfo, String tradeType) {
        pageInfo = userAccountDetailMapper.getUserAccountDetail(tradeType, pageInfo, userId);
        return pageInfo;
    }
}
