package com.atguigu.tingshu.payment.service;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface WxPayService {


    /**
     * 通过微信进行商品支付
     *
     * @param userId
     * @param paymentType
     * @param orderNo
     * @return
     */
    Map<String, String> createJsapi(Long userId, String paymentType, String orderNo);

    Boolean queryPayStatus(String orderNo);

    Map<String, String> wxNotify(HttpServletRequest request);
}
