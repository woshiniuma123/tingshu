package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.cache.GuiGuCache;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.atguigu.tingshu.common.constant.SystemConstant.*;


@Slf4j
@Service
@SuppressWarnings({"all"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

    @Autowired
    private AlbumInfoMapper albumInfoMapper;
    @Autowired
    private AlbumAttributeValueService albumAttributeValueService;
    @Autowired
    private AlbumStatMapper albumStatMapper;
    @Autowired
    private TrackInfoMapper trackInfoMapper;
    @Autowired
    private VodService vodService;
    @Autowired
    private RabbitService rabbitService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAlbumInfo(AlbumInfoVo albumInfoVo) {
        //1.向albuminfo表中插入信息
        AlbumInfo albumInfo = BeanUtil.copyProperties(albumInfoVo, AlbumInfo.class);
        //1.1从当前线程中获取用户id
        Long userId = AuthContextHolder.getUserId();
        albumInfo.setUserId(userId);
        //1.2判断当前专辑的付费类型
        String payType = albumInfo.getPayType();
        if (ALBUM_PAY_TYPE_REQUIRE.equals(payType) || ALBUM_PAY_TYPE_VIPFREE.equals(payType)) {
            //如果是vip免费和付费的设置可以试听的集数
            albumInfo.setTracksForFree(3);
        }
        //设置审核状态
        albumInfo.setStatus(ALBUM_STATUS_NO_PASS);
        albumInfoMapper.insert(albumInfo);
        Long albumInfoId = albumInfo.getId();
        //2.向albumattributevalue表中插入信息
        List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
        List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueVoList.stream()
                .map(albumAttributeValueVo -> {
                    AlbumAttributeValue albumAttributeValue = BeanUtil.copyProperties(albumAttributeValueVo, AlbumAttributeValue.class);
                    albumAttributeValue.setAlbumId(albumInfoId);
                    albumAttributeValue.setValueId(albumAttributeValueVo.getValueId());
                    return albumAttributeValue;
                }).collect(Collectors.toList());
        albumAttributeValueService.saveBatch(albumAttributeValueList);
        //3.向albumstate表中插入信息
        this.saveAlbumInfoStat(albumInfoId, ALBUM_STAT_PLAY, 0);
        this.saveAlbumInfoStat(albumInfoId, ALBUM_STAT_SUBSCRIBE, 0);
        this.saveAlbumInfoStat(albumInfoId, ALBUM_STAT_BUY, 0);
        this.saveAlbumInfoStat(albumInfoId, ALBUM_STAT_COMMENT, 0);
        //进行文本审核
        String text = albumInfo.getAlbumTitle() + albumInfo.getAlbumIntro();
        String suggestion = vodService.AuditText(text);
        if ("block".equals(suggestion)) {
            albumInfo.setStatus(ALBUM_STATUS_NO_PASS);
        } else if ("review".equals(suggestion)) {
            albumInfo.setStatus(ALBUM_STATUS_ARTIFICIAL);
        } else if ("pass".equals(suggestion)) {
            albumInfo.setStatus(ALBUM_STATUS_PASS);
            //TODO 对审核通过的专辑进行上架
            rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_UPPER, albumInfoId);
        }
        albumInfoMapper.updateById(albumInfo);
    }

    @Override
    public void saveAlbumInfoStat(Long albumId, String statType, Integer statNum) {
        AlbumStat albumStat = new AlbumStat();
        albumStat.setAlbumId(albumId);
        albumStat.setStatType(statType);
        albumStat.setStatNum(statNum);
        albumStatMapper.insert(albumStat);
    }

    @Override
    public IPage<AlbumListVo> findUserAlbumPageByUserId(IPage<AlbumListVo> pageInfo, AlbumInfoQuery albumInfoQuery) {
        IPage<AlbumListVo> page = albumInfoMapper.findUserAlbumPageByUserId(pageInfo, albumInfoQuery);
        return page;
    }

    /**
     * 根据专辑id删除专辑信息以及其相关信息
     *
     * @param id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAlbumInfo(Long id) {
        //1.先检查该专辑下是否有关联的声音
        Long count = trackInfoMapper.selectCount(new LambdaQueryWrapper<TrackInfo>()
                .eq(TrackInfo::getAlbumId, id).eq(TrackInfo::getIsDeleted, 0));
        if (count > 0) {
            //有关联的声音
            throw new RuntimeException("该专辑下有关联的声音，请先删除该专辑下的声音");
        }
        try {
            //2.如果没有关联的声音则删除专辑信息
            albumInfoMapper.deleteById(id);
            //3.删除专辑属性值关联表的信息
            albumAttributeValueService.remove(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, id));
            //4.删除专辑统计信息
            albumStatMapper.delete(new LambdaQueryWrapper<AlbumStat>().eq(AlbumStat::getAlbumId, id));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据id获取专辑信息和属性值
     *
     * @param id
     * @return
     */
 /*   @Override
    public AlbumInfo getAlbumInfoById(Long id) {

        try {
            //1.先从redis中获取数据，如果redis中存在数据则直接返回
            //1.1构建redisKey
            String redisKey = RedisConstant.ALBUM_INFO_PREFIX + id;
            //1.2从redis中获取数据
            AlbumInfo albumInfo = (AlbumInfo) redisTemplate.opsForValue().get(redisKey);
            if (albumInfo != null) {
                //缓存中存在数据
                return albumInfo;
            }
            //2.如果redis中不存在数据，则从数据库中查询 分布式锁防止缓存击穿
            //2.1构建分布式锁的lockKey
            String lockKey = redisKey + RedisConstant.CACHE_LOCK_SUFFIX;
            //2.2构建分布式锁对象
            RLock lock = redissonClient.getLock(lockKey);
            //2.3获取锁，如果获取到了则执行查库，并将查询到的结果存入redis中
            boolean flag = lock.tryLock();
            if (flag) {
                try {
                    AlbumInfo albumInfoFromDB = this.getAlbumInfoByIdFromDB(id);
                    //2.将数据存入缓存中
                    int ttl = RandomUtil.randomInt(500, 600);
                    //防止缓存雪崩
                    redisTemplate.opsForValue().set(redisKey, albumInfoFromDB, RedisConstant.ALBUM_TIMEOUT + ttl, TimeUnit.SECONDS);
                    return albumInfoFromDB;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    lock.unlock();
                }
            } else {
                //2.4如果没有获取到，则自旋
                return this.getAlbumInfoById(id);
            }
        } catch (RuntimeException e) {
            //如果缓存中没有数据，且获取分布式锁也失败了返回兜底数据
            return this.getAlbumInfoByIdFromDB(id);
        }
    }
*/

    /**
     * 从数据库中获取专辑详情
     *
     * @param id
     * @return
     */
    @GuiGuCache(prefix = RedisConstant.ALBUM_INFO_PREFIX)
    @Override
    public AlbumInfo getAlbumInfoById(Long id) {
        AlbumInfo albumInfo = albumInfoMapper.getAlbumInfoById(id);
        return albumInfo;
    }


    /**
     * 更新专辑信息
     *
     * @param id
     * @param albumInfoVo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAlbumInfo(Long id, AlbumInfoVo albumInfoVo) {

        //1.更新专辑信息表
        //1.1 从当前线程中获取用户id
        Long userId = AuthContextHolder.getUserId();
        //1.2将albuminfoVo中的属性值复制到AlbumInfo中
        AlbumInfo albumInfo = BeanUtil.copyProperties(albumInfoVo, AlbumInfo.class);
        albumInfo.setId(id);
        albumInfo.setUserId(userId);
        //专辑状态改为未审核
        albumInfo.setStatus(ALBUM_STATUS_NO_PASS);
        //1.3根据id更新专辑信息表
        albumInfoMapper.updateById(albumInfo);
        //2.更新专辑属性值关联表
        //2.1获取albuminfoVo中的AlbumAttributeValueVo类型的列表
        List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
        List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueVoList.stream()
                .map(albumAttributeValueVo -> {
                    AlbumAttributeValue albumAttributeValue = BeanUtil.copyProperties(albumAttributeValueVo, AlbumAttributeValue.class);
                    albumAttributeValue.setAlbumId(id);
                    return albumAttributeValue;
                }).collect(Collectors.toList());
        //2.2删除专辑属性值关联表信息根据专辑id
        albumAttributeValueService.remove(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, id));
        albumAttributeValueService.saveBatch(albumAttributeValueList);
        //进行文本审核
        String text = albumInfo.getAlbumTitle() + albumInfo.getAlbumIntro();
        String suggestion = vodService.AuditText(text);
        if ("block".equals(suggestion)) {
            albumInfo.setStatus(ALBUM_STATUS_NO_PASS);
            //TODO 如果修改后审核不通过了则进行下架
            rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_LOWER, id);
        } else if ("review".equals(suggestion)) {
            albumInfo.setStatus(ALBUM_STATUS_ARTIFICIAL);
        } else if ("pass".equals(suggestion)) {
            albumInfo.setStatus(ALBUM_STATUS_PASS);
            //TODO 对修改后审核通过的专辑进行上架
            rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_UPPER, id);
        }
        albumInfoMapper.updateById(albumInfo);
    }

    /**
     * 查询当前用户所有的专辑列表
     *
     * @param userId
     * @return
     */
    @GuiGuCache(prefix = "user:album:")
    @Override
    public List<AlbumInfo> findUserAllAlbumList(Long userId) {
        List<AlbumInfo> albumInfos = albumInfoMapper
                .selectList(new LambdaQueryWrapper<AlbumInfo>()
                        .select(AlbumInfo::getId, AlbumInfo::getAlbumTitle)
                        .eq(AlbumInfo::getUserId, userId)
                        .eq(AlbumInfo::getIsDeleted, 0)
                        .orderByDesc(AlbumInfo::getId)
                        .last("limit 200"));
        return albumInfos;
    }

    /**
     * 根据专辑id获取专辑的统计信息
     *
     * @param albumId
     * @return
     */
    @Override
    public AlbumStatVo getAlbumStatVo(Long albumId) {
        AlbumStatVo albumStatVo = albumInfoMapper.getAlbumStatVo(albumId);
        return albumStatVo;
    }

    /**
     * 项目维护期间重建布隆过滤器
     */
    @Override
    public void rebuildBloom() {
        //1.获取原有的布隆过滤器对象得到配置信息
        RBloomFilter<Object> oldBloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        long count = oldBloomFilter.count();
        long expectedInsertions = oldBloomFilter.getExpectedInsertions();
        double falseProbability = oldBloomFilter.getFalseProbability();
        //2.判断布隆过滤器现有的数量是否大于期望的数量
        if (count > expectedInsertions) {
            oldBloomFilter.delete();
            RBloomFilter<Object> newBloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER + ":new");
            newBloomFilter.tryInit(expectedInsertions * 2, falseProbability);
            //2.查询数据库中过审的专辑id存入新的布隆过滤器
            List<AlbumInfo> albumInfoIds = albumInfoMapper
                    .selectList(new LambdaQueryWrapper<AlbumInfo>()
                            .eq(AlbumInfo::getStatus, ALBUM_STATUS_PASS).select(AlbumInfo::getId));
            if (CollectionUtil.isNotEmpty(albumInfoIds)) {
                for (AlbumInfo albumInfoId : albumInfoIds) {
                    newBloomFilter.add(albumInfoId);
                }
                newBloomFilter.rename(RedisConstant.ALBUM_BLOOM_FILTER);
            }
        } else {
            //4.未超过：重建即可
            //4.1 删除旧布隆过滤器
            oldBloomFilter.delete();
            //4.1 创建初始化新布隆过滤器 旧期望数据规模*2 其他配置保留
            RBloomFilter<Long> newBloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER + ":new");
            newBloomFilter.tryInit(expectedInsertions * 2, falseProbability);

            //4.2 查询DB过审专辑ID列表，将专辑ID存入新布隆过滤器
            List<AlbumInfo> list = this.list(
                    new LambdaQueryWrapper<AlbumInfo>()
                            .eq(AlbumInfo::getStatus, SystemConstant.ALBUM_STATUS_PASS)
                            .select(AlbumInfo::getId)
            );
            if (CollUtil.isNotEmpty(list)) {
                for (AlbumInfo albumInfo : list) {
                    newBloomFilter.add(albumInfo.getId());
                }
                //3.3 重命名改为原来布隆过滤器名称
                newBloomFilter.rename(RedisConstant.ALBUM_BLOOM_FILTER);
            }
        }
    }
}
