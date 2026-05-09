package com.atguigu.tingshu.user.pattern;

import com.atguigu.tingshu.vo.user.UserPaidRecordVo;

public interface DeliveryStrategy {

    void delivery(UserPaidRecordVo userPaidRecordVo);
}
