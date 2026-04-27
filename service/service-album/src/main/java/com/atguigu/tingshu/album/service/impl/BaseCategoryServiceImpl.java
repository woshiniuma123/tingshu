package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"all"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;
    @Autowired
    private BaseAttributeMapper baseAttributeMapper;

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

    @Override
    public List<JSONObject> getAttributeByCategory1Id(Long category1Id) {

        List<BaseAttribute> baseAttributes = baseAttributeMapper.getAttributeByCategory1Id(category1Id);
        //1.创建返回结果的jsonlist
        List<JSONObject> resultList = new ArrayList<>();
        baseAttributes.forEach(baseAttribute -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", baseAttribute.getId());
            jsonObject.put("createTime", baseAttribute.getCreateTime());
            jsonObject.put("category1Id", baseAttribute.getCategory1Id());
            jsonObject.put("attributeName", baseAttribute.getAttributeName());
            jsonObject.put("attributeValueList", baseAttribute.getAttributeValueList());
            resultList.add(jsonObject);
        });
        return resultList;
    }

    @Override
    public BaseCategoryView getCategoryViewByCategory3Id(Long category3Id) {
        BaseCategoryView baseCategoryView = baseCategoryViewMapper.getCategoryViewByCategory3Id(category3Id);
        return baseCategoryView;
    }

    /**
     * 根据分类1的id查询分类3的信息
     *
     * @param category1Id
     * @return
     */
    @Override
    public List<BaseCategory3> findTopBaseCategory3(Long category1Id) {
        List<BaseCategory3> baseCategory3 = baseCategory3Mapper.findTopBaseCategory3(category1Id);
        return baseCategory3;
    }

    /**
     * 根据分类1的id查询其
     *
     * @param category1Id
     * @return
     */
    @Override
    public JSONObject getBaseCategoryListByCategory1Id(Long category1Id) {

        //1.查询当前分类id的全部分类信息
        List<BaseCategoryView> baseCategory1Views = baseCategoryViewMapper.selectList(new LambdaQueryWrapper<BaseCategoryView>()
                .eq(BaseCategoryView::getCategory1Id, category1Id));
        if (CollectionUtil.isNotEmpty(baseCategory1Views)) {
            //2创建分类1的对象
            JSONObject jsonObject1 = new JSONObject();
            jsonObject1.put("categoryId", baseCategory1Views.get(0).getCategory1Id());
            jsonObject1.put("categoryName", baseCategory1Views.get(0).getCategory1Name());
            //3.根据分类2id进行分组
            Map<Long, List<BaseCategoryView>> group1 = baseCategory1Views.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            Set<Map.Entry<Long, List<BaseCategoryView>>> entries1 = group1.entrySet();
            //4.构建分类2的集合列表
            ArrayList<JSONObject> jsonObject2List = new ArrayList<>();
            for (Map.Entry<Long, List<BaseCategoryView>> entry1 : entries1) {
                JSONObject jsonObject2 = new JSONObject();
                jsonObject2.put("categoryId", entry1.getKey());
                jsonObject2.put("categoryName", entry1.getValue().get(0).getCategory2Name());
                //构建三级分类列表
                ArrayList<JSONObject> jsonObjects3List = new ArrayList<>();
                for (BaseCategoryView baseCategoryView : entry1.getValue()) {
                    JSONObject jsonObject3 = new JSONObject();
                    jsonObject3.put("categoryId", baseCategoryView.getCategory3Id());
                    jsonObject3.put("categoryName", baseCategoryView.getCategory3Name());
                    jsonObjects3List.add(jsonObject3);
                }
                //5.给分类2添加分类3的集合列表
                jsonObject2.put("categoryChild", jsonObjects3List);
                //6.将每个分类2的对象添加到分类2列表
                jsonObject2List.add(jsonObject2);
            }
            jsonObject1.put("categoryChild", jsonObject2List);
            return jsonObject1;
        }
        return null;
    }
}
