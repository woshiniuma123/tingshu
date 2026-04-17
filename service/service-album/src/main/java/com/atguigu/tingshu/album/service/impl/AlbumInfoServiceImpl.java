package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.BaseCategoryViewMapper;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

    @Autowired
    private AlbumInfoMapper albumInfoMapper;
    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Override
    public List<JSONObject> getBaseCategoryList() {
        List<BaseCategoryView> baseCategoryViews = baseCategoryViewMapper.selectList(null);
        //1处理分类id1
        //1.1创建分类1列表
        List<JSONObject> list1 = new ArrayList<>();
        //对分类1的列表按照category1Id进行分组
        Map<Long, List<BaseCategoryView>> groupByCategory1Id = baseCategoryViews.stream().
                collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        //1.2遍历按照分类1Id分组的map集合
        for (Map.Entry<Long, List<BaseCategoryView>> entry1 : groupByCategory1Id.entrySet()) {
            //1.3创建category1Id对应的json对象
            JSONObject jsonObject1 = new JSONObject();
            jsonObject1.put("categoryId", entry1.getKey());
            jsonObject1.put("categoryName", entry1.getValue().get(0).getCategory1Name());
            //2处理categoryid的结果
            //2.1创建category2Id对应的json列表
            List<JSONObject> list2 = new ArrayList<>();
            //2.2按照category2Id进行分组
            Map<Long, List<BaseCategoryView>> groupByCategory2Id = entry1.getValue().stream().
                    collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            //2.3遍历按照category2Id分组的map集合
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : groupByCategory2Id.entrySet()) {
                //2.4创建category2Id对应的json对象
                JSONObject jsonObject2 = new JSONObject();
                jsonObject2.put("categoryId", entry2.getKey());
                jsonObject2.put("categoryName", entry2.getValue().get(0).getCategory2Name());

                //3.1创建category3Id对应的json列表
                List<JSONObject> list3 = new ArrayList<>();
                //3.2遍历category3Id对应的数据列表
                entry2.getValue().forEach(baseCategoryView -> {
                    JSONObject jsonObject3 = new JSONObject();
                    jsonObject3.put("categoryId", baseCategoryView.getCategory3Id());
                    jsonObject3.put("categoryName", baseCategoryView.getCategory3Name());
                    list3.add(jsonObject3);
                });
                jsonObject2.put("categoryChild", list3);
                list2.add(jsonObject2);

            }
            jsonObject1.put("categoryChild", list2);
            list1.add(jsonObject1);
        }
        return list1;
    }
}
