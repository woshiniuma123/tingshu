package com.atguigu.tingshu.user.service.impl;

import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.mapper.VipServiceConfigMapper;
import com.atguigu.tingshu.user.service.VipServiceConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@SuppressWarnings({"all"})
public class VipServiceConfigServiceImpl extends ServiceImpl<VipServiceConfigMapper, VipServiceConfig> implements VipServiceConfigService {

    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;

    /**
     * 获取vip套餐
     *
     * @return
     */
    @Override
    public List<VipServiceConfig> findAll() {
        List<VipServiceConfig> vipServiceConfigs = vipServiceConfigMapper.selectList(null);
        return vipServiceConfigs;
    }
}
