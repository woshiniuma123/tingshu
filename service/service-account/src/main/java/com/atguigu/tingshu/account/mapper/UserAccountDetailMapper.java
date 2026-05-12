package com.atguigu.tingshu.account.mapper;

import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserAccountDetailMapper extends BaseMapper<UserAccountDetail> {

    Page<UserAccountDetail> getUserAccountDetail(String tradeType, Page<UserAccountDetail> pageInfo, Long userId);
}
