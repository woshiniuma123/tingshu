package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface VipServiceConfigService extends IService<VipServiceConfig> {

    List<VipServiceConfig> findAll();
}
