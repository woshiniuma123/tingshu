package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BaseCategoryViewMapper extends BaseMapper<BaseCategoryView> {

    BaseCategoryView getCategoryViewByCategory3Id(@Param("category3Id") Long category3Id);
}
