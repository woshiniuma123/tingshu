package com.atguigu.tingshu.dispatch.job;

import com.atguigu.tingshu.search.client.SearchFeignClient;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DispatchHandler {


    @XxlJob("test")
    public void test() {
        log.info("测试定时任务");
    }


    @Autowired
    private SearchFeignClient searchFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;

    /**
     * 定时更新专辑小时排行榜
     */
    @XxlJob("updateLatelyRankList")
    public void updateLatelyRankList() {
        log.info("更新小时榜开始");
        searchFeignClient.updateLatelyAlbumRanking();
        log.info("更新小时榜成功");
    }

    /**
     * 定时查询vip已经到期的用户并修改用户的vip状态
     */
    @XxlJob("updateUserVipStatus")
    public void updateUserVipStatus() {
        log.info("开始查询并更新用户的vip状态");
        userFeignClient.updateUserVipStatus();
        log.info("更新用户的vip状态完毕");
    }

}