package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.BaseCategory3;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BaseCategory3Mapper extends BaseMapper<BaseCategory3> {


    List<BaseCategory3> findTopBaseCategory3(Long category1Id);
}
