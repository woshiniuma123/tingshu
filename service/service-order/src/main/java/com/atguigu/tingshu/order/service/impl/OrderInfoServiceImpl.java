package com.atguigu.tingshu.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.account.AccountFeignClient;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.order.OrderDerate;
import com.atguigu.tingshu.model.order.OrderDetail;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderDerateMapper;
import com.atguigu.tingshu.order.mapper.OrderDetailMapper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.pattern.TradeStrategy;
import com.atguigu.tingshu.order.pattern.factory.TradeStrategyFactory;
import com.atguigu.tingshu.order.service.OrderDerateService;
import com.atguigu.tingshu.order.service.OrderDetailService;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private AccountFeignClient accountFeignClient;
    @Autowired
    private TradeStrategyFactory tradeStrategyFactory;

    /**
     * 获取订单数据汇总
     *
     * @param tradeVo
     * @param trackCount
     * @return
     */
    @Override
    public OrderInfoVo trade(TradeVo tradeVo, Long userId, Integer trackCount) {
       /* //付款类型id
        Long itemId = tradeVo.getItemId();
        //专辑付款类型 付款项目类型: 1001-专辑 1002-声音 1003-vip会员
        String itemType = tradeVo.getItemType();
        //0.0初始化orderinfoVo
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        //0.1初始化订单明细列表
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();
        //0.2初始化订单减免列表
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();
        //0.3初始化订单原始金额
        BigDecimal originalAmount = new BigDecimal("0.00");
        BigDecimal derateAmount = new BigDecimal("0.00");
        BigDecimal orderAmount = new BigDecimal("0.00");

        //1.如果付款类型是vip会员
        if (SystemConstant.ORDER_ITEM_TYPE_VIP.equals(itemType)) {
            VipServiceConfig vipServiceConfig = userFeignClient.getVipServiceConfig(itemId).getData();
            originalAmount = vipServiceConfig.getPrice();
            orderAmount = vipServiceConfig.getDiscountPrice();
            derateAmount = originalAmount.subtract(orderAmount);
            //构建订单明细
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(itemId);
            orderDetailVo.setItemName(vipServiceConfig.getName());
            orderDetailVo.setItemPrice(originalAmount);
            orderDetailVo.setItemUrl(vipServiceConfig.getImageUrl());
            orderDetailVoList.add(orderDetailVo);
            orderInfoVo.setOrderDetailVoList(orderDetailVoList);

            if (originalAmount.compareTo(orderAmount) == 1) {
                //构建订单折扣列表
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateAmount(derateAmount);
                orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_VIP_SERVICE_DISCOUNT);
                orderDerateVo.setRemarks("VIP充值折扣");
                orderDerateVoList.add(orderDerateVo);
                orderInfoVo.setOrderDerateVoList(orderDerateVoList);
            }

        } else if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(itemType)) {
            //2.如果专辑购买
            //判断当前用户是否购买过该专辑
            Boolean flag = userFeignClient.isPaidAlbum(itemId).getData();
            if (flag) {
                //该用户购买过该专辑
                throw new GuiguException(500, "您已经购买过该专辑专辑了" + itemId);
            }
            //2.1远程调用专辑微服务根据专辑id获取信息
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(itemId).getData();
            Assert.notNull(albumInfo, "该专辑{}的信息为空", itemId);
            //2.2设置专辑原价
            originalAmount = albumInfo.getPrice();
            derateAmount = originalAmount;
            orderAmount = originalAmount;
            //2.3远程调用用户微服务获取当前用户的信息
            UserInfoVo userInfo = userFeignClient.getUserInfo(userId).getData();
            //2.4判断用户是普通用户还是vip用户
            boolean isVIP = false;
            if (userInfo.getIsVip() == 1 && userInfo.getVipExpireTime().after(new Date())) {
                isVIP = true;
            }
            //2.5用户不是vip
            if (!isVIP && albumInfo.getDiscount().intValue() != -1) {
                orderAmount = originalAmount.multiply(albumInfo.getDiscount())
                        .divide(new BigDecimal("10.0"), 2, RoundingMode.HALF_UP);
            }
            //2.6用户是vip
            if (isVIP && albumInfo.getVipDiscount().intValue() != -1) {
                orderAmount = originalAmount.multiply(albumInfo.getVipDiscount())
                        .divide(new BigDecimal("10.0"), 2, RoundingMode.HALF_UP);
            }
            //折扣金额
            derateAmount = originalAmount.subtract(orderAmount);
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemUrl(albumInfo.getCoverUrl());
            orderDetailVo.setItemPrice(originalAmount);
            orderDetailVo.setItemId(itemId);
            orderDetailVo.setItemName("专辑名称:" + albumInfo.getAlbumTitle());
            orderDetailVoList.add(orderDetailVo);
            orderInfoVo.setOrderDetailVoList(orderDetailVoList);

            if (orderAmount.compareTo(orderAmount) == 1) {
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_ALBUM_DISCOUNT);
                orderDerateVo.setDerateAmount(derateAmount);
                orderDerateVo.setRemarks("限时减免");
                orderDerateVoList.add(orderDerateVo);
                orderInfoVo.setOrderDerateVoList(orderDerateVoList);
            }

        } else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(itemType)) {
            //3.如果付款类型是声音
            //3.1远程调用专辑微服务获取用户未购买的声音列表
            List<TrackInfo> trackInfoList = albumFeignClient.findPaidTrackInfoList(itemId, trackCount).getData();
            Assert.notNull(trackInfoList, "用户已经购买了全部声音");
            //3.2远程调用专辑微服务获取声音所在专辑的相关信息
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(trackInfoList.get(0).getAlbumId()).getData();
            //获取到每个声音的价格
            BigDecimal price = albumInfo.getPrice();
            originalAmount = price.multiply(BigDecimal.valueOf(trackCount));
            orderAmount = price.multiply(BigDecimal.valueOf(trackCount));

            List<OrderDetailVo> orderDetailVoList1 = trackInfoList.stream().map(
                    trackInfo -> {
                        OrderDetailVo orderDetailVo = new OrderDetailVo();
                        orderDetailVo.setItemName(trackInfo.getTrackTitle());
                        orderDetailVo.setItemId(trackInfo.getId());
                        orderDetailVo.setItemPrice(price);
                        orderDetailVo.setItemUrl(trackInfo.getCoverUrl());
                        return orderDetailVo;
                    }
            ).collect(Collectors.toList());

            orderInfoVo.setOrderDetailVoList(orderDetailVoList1);
        }


        //4.返回结果
        orderInfoVo.setItemType(itemType);
        orderInfoVo.setOriginalAmount(originalAmount);
        orderInfoVo.setDerateAmount(derateAmount);
        orderInfoVo.setOrderAmount(orderAmount);
//        orderInfoVo.setOrderDerateVoList(orderDerateVoList);
//        orderInfoVo.setOrderDetailVoList(orderDetailVoList);
        orderInfoVo.setTimestamp(System.currentTimeMillis());
        orderInfoVo.setItemType(itemType);
        //设置交易号 防止重复提交
        String tradeKey = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
        String tradeNo = IdUtil.randomUUID();
        redisTemplate.opsForValue().set(tradeKey, tradeNo, 5, TimeUnit.MINUTES);
        orderInfoVo.setTradeNo(tradeNo);
        //设置签名 避免抓包修改
        Map<String, Object> orderInfoMap = BeanUtil.beanToMap(orderInfoVo, false, true);
        String sign = SignHelper.getSign(orderInfoMap);
        orderInfoVo.setSign(sign);
        return orderInfoVo;*/
        TradeStrategy strategy = tradeStrategyFactory.getStrategy(tradeVo.getItemType());
        OrderInfoVo orderInfoVo = strategy.trade(tradeVo, userId);
        return orderInfoVo;

    }

    @Autowired
    private RabbitService rabbitService;

    /**
     * 提交订单
     *
     * @param orderInfoVo
     * @param userId
     * @return
     */
    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public Map<String, String> submitOrder(OrderInfoVo orderInfoVo, Long userId) {

        String tradeNo = orderInfoVo.getTradeNo();
        String sign = orderInfoVo.getSign();
        //1.根据交易号判断该订单是否有效
        String tradeKey = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
        String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                "then\n" +
                "    return redis.call(\"del\",KEYS[1])\n" +
                "else\n" +
                "    return 0\n" +
                "end";
        DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>(script, Boolean.class);
        Boolean flag = (Boolean) redisTemplate.execute(redisScript, Arrays.asList(tradeKey), tradeNo);
        if (!flag) {
            throw new GuiguException(500, "订单已经过期，或订单已经提交");
        }
        try {
            //2.根据签名判断订单信息是否被恶意修改
            Map<String, Object> orderInfoMap = BeanUtil.beanToMap(orderInfoVo, false, true);
            orderInfoMap.remove("payWay");
            SignHelper.checkSign(orderInfoMap);
        } catch (Exception e) {
            throw new GuiguException(500, e.getMessage());
        }
        //3.提交订单以及订单详情信息和折扣信息信息
        OrderInfo orderInfo = this.saveOrder(orderInfoVo, userId);
        //4.用户支付 '支付方式：1101-微信 1102-支付宝 1103-账户余额'
        //获取支付类型
        String payWay = orderInfoVo.getPayWay();
        //如果付款类型是余额支付
        if (SystemConstant.ORDER_PAY_ACCOUNT.equals(payWay)) {
            //4.1扣减账户余额
            AccountDeductVo accountDeductVo = new AccountDeductVo();
            accountDeductVo.setAmount(orderInfo.getOrderAmount());
            accountDeductVo.setUserId(orderInfo.getUserId());
            accountDeductVo.setContent(orderInfo.getOrderTitle());
            accountDeductVo.setOrderNo(orderInfo.getOrderNo());
            Result result = accountFeignClient.checkAndDeduct(accountDeductVo);
            if (result.getCode().intValue() != 200) {
                //4.2 扣减余额失败，业务终止，回滚全局事务
                throw new GuiguException(result.getCode(), result.getMessage());
            }
            //4.2修改订单状态
            orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_PAID);
            orderInfoMapper.updateById(orderInfo);
            //4.3给用户发放权益 远程调用用户微服务
            UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
            userPaidRecordVo.setItemType(orderInfo.getItemType());
            userPaidRecordVo.setOrderNo(orderInfo.getOrderNo());
            userPaidRecordVo.setUserId(orderInfo.getUserId());
            List<OrderDetailVo> orderDetailVoList = orderInfoVo.getOrderDetailVoList();
            if (CollectionUtil.isNotEmpty(orderDetailVoList)) {
                List<Long> itemIdList = orderDetailVoList.stream().map(orderDetailVo ->
                        orderDetailVo.getItemId()
                ).collect(Collectors.toList());
                userPaidRecordVo.setItemIdList(itemIdList);
                result = userFeignClient.savePaidRecord(userPaidRecordVo);
                if (result.getCode().intValue() != 200) {
                    //4.2 虚拟物品发货失败，业务终止，回滚全局事务
                    throw new GuiguException(result.getCode(), result.getMessage());
                }
            }
//            int i = 1 / 0;
        }

        //如果支付方式是微信支付调用rabbitmq发送延迟关闭订单的消息
        if (SystemConstant.ORDER_PAY_WAY_WEIXIN.equals(payWay)) {

            rabbitService.sendDealyMessage(MqConst.EXCHANGE_CANCEL_ORDER, MqConst.ROUTING_CANCEL_ORDER, orderInfo.getId(), cancelTTL);

        }
        //通过rabbitmq发送延迟消息进行延迟关闭订单 时间为15分钟
        Map<String, String> map = new HashMap<>();
        map.put("orderNo", orderInfo.getOrderNo());
        return map;
    }

    @Value("${order.cancel}")
    private Integer cancelTTL;
    @Autowired
    private OrderDetailService orderDetailService;
    @Autowired
    private OrderDerateService orderDerateService;

    /**
     * 保存订单信息
     *
     * @param orderInfoVo
     * @param userId
     */
    @Override
    public OrderInfo saveOrder(OrderInfoVo orderInfoVo, Long userId) {
        OrderInfo orderInfo = BeanUtil.copyProperties(orderInfoVo, OrderInfo.class);
        //1.提交订单信息
        orderInfo.setUserId(userId);
        //1.2设置订单号
        String orderNo = DateUtil.today().replaceAll("-", "") + IdUtil.getSnowflakeNextId();
        orderInfo.setOrderNo(orderNo);
        //1.3设置订单标题
        List<OrderDetailVo> orderDetailVoList = orderInfoVo.getOrderDetailVoList();
        if (CollectionUtil.isNotEmpty(orderDetailVoList)) {
            orderInfo.setOrderTitle(orderDetailVoList.get(0).getItemName());
        }
        //1.4设置订单状态
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_UNPAID);
        //1.5保存订单信息
        orderInfoMapper.insert(orderInfo);
        //1.6获取订单id
        Long orderId = orderInfo.getId();
        //2.提交订单明细信息
        if (CollectionUtil.isNotEmpty(orderDetailVoList)) {
            List<OrderDetail> orderDetailList = orderDetailVoList.stream().map(orderDetailVo -> {
                        OrderDetail orderDetail = BeanUtil.copyProperties(orderDetailVo, OrderDetail.class);
                        orderDetail.setOrderId(orderId);
                        return orderDetail;
                    }
            ).collect(Collectors.toList());
            orderDetailService.saveBatch(orderDetailList);
        }
        //3.提交订单折扣信息
        List<OrderDerateVo> orderDerateVoList = orderInfoVo.getOrderDerateVoList();
        if (CollectionUtil.isNotEmpty(orderDerateVoList)) {
            List<OrderDerate> orderDerateList = orderDerateVoList.stream().map(orderDerateVo -> {
                OrderDerate orderDerate = BeanUtil.copyProperties(orderDerateVo, OrderDerate.class);
                orderDerate.setOrderId(orderId);
                return orderDerate;
            }).collect(Collectors.toList());
            orderDerateService.saveBatch(orderDerateList);
        }
        return orderInfo;
    }

    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private OrderDerateMapper orderDerateMapper;

    /**
     * 根据订单号获取订单信息
     *
     * @param orderNo
     * @return
     */
    @Override
    public OrderInfo getOrderInfo(String orderNo) {
        //1.根据订单号查询订单信息
        OrderInfo orderInfo = orderInfoMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getOrderNo, orderNo)
        );
        Long orderId = orderInfo.getId();
        //2.根据订单id查询订单明细
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(
                new LambdaQueryWrapper<OrderDetail>()
                        .eq(OrderDetail::getOrderId, orderId));
        if (CollectionUtil.isNotEmpty(orderDetails)) {
            orderInfo.setOrderDetailList(orderDetails);
        }
        //3.根据订单id查询订单明细
        List<OrderDerate> orderDerates = orderDerateMapper.selectList(
                new LambdaQueryWrapper<OrderDerate>()
                        .eq(OrderDerate::getOrderId, orderId)
        );
        if (CollectionUtil.isNotEmpty(orderDerates)) {
            orderInfo.setOrderDerateList(orderDerates);
        }
        return orderInfo;
    }

    /**
     * 分页查询当前用户的订单列表
     *
     * @param pageInfo
     * @param userId
     * @return
     */
    @Override
    public Page<OrderInfo> findUserPage(Page<OrderInfo> pageInfo, Long userId) {
        pageInfo = orderInfoMapper.selectUserPage(pageInfo, userId);
        List<OrderInfo> orderInfoList = pageInfo.getRecords();
        List<Long> orderIdList = orderInfoList.stream().map(OrderInfo::getId).collect(Collectors.toList());
        //根据订单id对订单明细进行分组
        Map<Long, List<OrderDetail>> orderDetailList = orderDetailMapper.selectList(
                new LambdaQueryWrapper<OrderDetail>()
                        .in(OrderDetail::getOrderId, orderIdList)).stream().collect(Collectors.groupingBy(OrderDetail::getOrderId));
        //根据订单id对订单减免进行分组
        Map<Long, List<OrderDerate>> orderDerateList = orderDerateMapper.selectList(
                new LambdaQueryWrapper<OrderDerate>()
                        .in(OrderDerate::getOrderId, orderIdList)
        ).stream().collect(Collectors.groupingBy(OrderDerate::getOrderId));
        //遍历订单列表给每个订单设置上订单明细列表和订单减免列表
        orderInfoList.forEach(
                orderInfo -> {
                    Long orderId = orderInfo.getId();
                    List<OrderDerate> orderDerates = orderDerateList.get(orderId);
                    orderInfo.setOrderDerateList(orderDerates);
                    List<OrderDetail> orderDetails = orderDetailList.get(orderId);
                    orderInfo.setOrderDetailList(orderDetails);
                }
        );
        pageInfo.setRecords(orderInfoList);
        return pageInfo;
    }

    /**
     * 取消订单
     *
     * @param orderId
     */
    @Override
    public void cancelOrder(Long orderId) {
        //1.开始执行取消订单的方法
        orderInfoMapper.update(null,
                new LambdaUpdateWrapper<OrderInfo>()
                        .eq(OrderInfo::getOrderStatus, SystemConstant.ORDER_STATUS_UNPAID)
                        .eq(OrderInfo::getId, orderId)
                        .set(OrderInfo::getOrderStatus, SystemConstant.ORDER_STATUS_CANCEL));
    }

    /**
     * 支付回调成功后更新订单状态,以及调用用户服务完成虚拟物品发货
     *
     * @param orderNo
     */
    @Override
    public void orderPaySuccess(String orderNo) {
        //1.更新订单的状态为已支付
        int flag = orderInfoMapper.update(
                null,
                new LambdaUpdateWrapper<OrderInfo>()
                        .eq(OrderInfo::getOrderNo, orderNo)
                        .eq(OrderInfo::getOrderStatus, SystemConstant.ORDER_STATUS_UNPAID)
                        .set(OrderInfo::getOrderStatus, SystemConstant.ORDER_STATUS_PAID)
        );
        //更新订单状态成功后
        if (flag > 0) {
            //2.查询订单信息
            OrderInfo orderInfo = orderInfoMapper.selectOne(
                    new LambdaQueryWrapper<OrderInfo>()
                            .eq(OrderInfo::getOrderNo, orderNo)
            );
            //2.进行虚拟物品发货 调用用户微服务
            UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
            userPaidRecordVo.setUserId(orderInfo.getUserId());
            userPaidRecordVo.setItemType(orderInfo.getItemType());
            //3.查询订单明细
            List<Long> orderItemIdList = orderDetailMapper.selectList(
                    new LambdaQueryWrapper<OrderDetail>()
                            .eq(OrderDetail::getOrderId, orderInfo.getId())
            ).stream().map(OrderDetail::getItemId).collect(Collectors.toList());
            userPaidRecordVo.setItemIdList(orderItemIdList);
            userPaidRecordVo.setOrderNo(orderNo);
            Result result = userFeignClient.savePaidRecord(userPaidRecordVo);
            if (result.getCode().intValue() != 200) {
                throw new GuiguException(500, result.getMessage());
            }

        }
    }

    @Autowired
    private RedisTemplate redisTemplate;

}

