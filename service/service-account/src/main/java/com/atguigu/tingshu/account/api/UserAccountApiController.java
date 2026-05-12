package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "用户账户管理")
@RestController
@RequestMapping("api/account")
@SuppressWarnings({"all"})
public class UserAccountApiController {

    @Autowired
    private UserAccountService userAccountService;

    /**
     * 获取当前用户的账户可用余额
     *
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "获取当前用户的账户可用余额")
    @GetMapping("/userAccount/getAvailableAmount")
    public Result<BigDecimal> getAvailableAmount() {
        Long userId = AuthContextHolder.getUserId();
        BigDecimal avaliableAmount = userAccountService.getAvailableAmount(userId);
        return Result.ok(avaliableAmount);
    }

    /**
     * 检查和扣减账户余额
     *
     * @param accountDeductVo
     * @return
     */
    @PostMapping("/userAccount/checkAndDeduct")
    @Operation(summary = "检查和扣减账户余额")
    public Result checkAndDeduct(@RequestBody AccountDeductVo accountDeductVo) {
        userAccountService.checkAndDeduct(accountDeductVo);
        return Result.ok();
    }

    @GuiGuLogin
    @Operation(summary = "获取当前用户的充值")
    @GetMapping("/userAccount/findUserRechargePage/{page}/{limit}")
    public Result<Page<UserAccountDetail>> findUserRechargePage(@PathVariable Integer page, @PathVariable Integer limit) {
        Long userId = AuthContextHolder.getUserId();
        Page<UserAccountDetail> pageInfo = new Page<>(page, limit);
        pageInfo = userAccountService.getUserAccountDetail(userId, pageInfo, SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT);
        return Result.ok(pageInfo);
    }

    @GuiGuLogin
    @Operation(summary = "获取当前用户的充值")
    @GetMapping("/userAccount/findUserConsumePage/{page}/{limit}")
    public Result<Page<UserAccountDetail>> findUserConsumePage(@PathVariable Integer page, @PathVariable Integer limit) {
        Long userId = AuthContextHolder.getUserId();
        Page<UserAccountDetail> pageInfo = new Page<>(page, limit);
        pageInfo = userAccountService.getUserAccountDetail(userId, pageInfo, SystemConstant.ACCOUNT_TRADE_TYPE_MINUS);
        return Result.ok(pageInfo);
    }
}

