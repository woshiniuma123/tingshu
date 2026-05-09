package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.extra.pinyin.PinyinUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import co.elastic.clients.json.JsonData;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.common.utils.StringUtils;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.model.album.*;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.model.search.SuggestIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.repository.AlbumInfoIndexRepository;
import com.atguigu.tingshu.search.repository.SuggestIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"all"})
public class SearchServiceImpl implements SearchService {

    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private AlbumInfoIndexRepository albumInfoIndexRepository;
    @Autowired
    private SuggestIndexRepository suggestIndexRepository;
    @Autowired
//    @Qualifier("threadPoolTaskExecutor")
    private Executor threadPoolTaskExecutor;

    /**
     * 根据商品id远程调用商品微服务，用户微服务获取对应信息来上架商品
     *
     * @param albumId
     */
    @Override
    public void upperAlbum(Long albumId) {
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();
        //1.远程调用专辑微服务获取专辑信息
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            Assert.notNull(albumInfo, "专辑不存在{}", albumId);
            BeanUtil.copyProperties(albumInfo, albumInfoIndex);
            List<AlbumAttributeValue> albumAttributeValueVoList = albumInfo.getAlbumAttributeValueVoList();
            List<AttributeValueIndex> attributeValueIndexList = albumAttributeValueVoList.stream().map(albumAttributeValue -> BeanUtil.copyProperties(albumAttributeValue, AttributeValueIndex.class)).collect(Collectors.toList());
            albumInfoIndex.setAttributeValueIndexList(attributeValueIndexList);
            return albumInfo;
        }, threadPoolTaskExecutor);

        //2.远程调用专辑微服务的获取分类信息
        CompletableFuture<Void> baseCategoryComoletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            BaseCategoryView baseCategoryView = albumFeignClient.getCategoryViewByCategory3Id(albumInfo.getCategory3Id()).getData();
            albumInfoIndex.setCategory1Id(baseCategoryView.getCategory1Id());
            albumInfoIndex.setCategory2Id(baseCategoryView.getCategory2Id());
        }, threadPoolTaskExecutor);


        //3.远程调用用户微服务获取用户基本信息
        CompletableFuture<Void> UserInfoCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            UserInfoVo userInfoVo = userFeignClient.getUserInfo(albumInfo.getUserId()).getData();
            albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());
        }, threadPoolTaskExecutor);

        // 4.远程调用专辑微服务获取专辑统计信息
        CompletableFuture<Void> StatCompletableFuture = CompletableFuture.runAsync(() -> {

            //远程调用专辑微服务获取专辑统计信息
            AlbumStatVo albumStatVo = albumFeignClient.getAlbumStatVo(albumId).getData();
            Integer playStatNum = albumStatVo.getPlayStatNum();
            Integer buyStatNum = albumStatVo.getBuyStatNum();
            Integer subscribeStatNum = albumStatVo.getSubscribeStatNum();
            Integer commentStatNum = albumStatVo.getCommentStatNum();
            //模拟播放量
//            Integer playStatNum = RandomUtil.randomInt(200, 300);
//            Integer subscribeStatNum = RandomUtil.randomInt(100, 200);
//            Integer buyStatNum = RandomUtil.randomInt(50, 100);
//            Integer commentStatNum = RandomUtil.randomInt(20, 50);
            albumInfoIndex.setPlayStatNum(playStatNum);
            albumInfoIndex.setSubscribeStatNum(subscribeStatNum);
            albumInfoIndex.setBuyStatNum(buyStatNum);
            albumInfoIndex.setCommentStatNum(commentStatNum);
            Double hotScore = BigDecimal.valueOf(playStatNum).multiply(BigDecimal.valueOf(0.1))
                    .add(BigDecimal.valueOf(subscribeStatNum).multiply(BigDecimal.valueOf(0.2)))
                    .add(BigDecimal.valueOf(buyStatNum).multiply(BigDecimal.valueOf(0.3)))
                    .add(BigDecimal.valueOf(commentStatNum).multiply(BigDecimal.valueOf(0.4))).doubleValue();
            albumInfoIndex.setHotScore(hotScore);
        }, threadPoolTaskExecutor);

        CompletableFuture.allOf(albumInfoCompletableFuture, baseCategoryComoletableFuture, UserInfoCompletableFuture, StatCompletableFuture).orTimeout(3, TimeUnit.SECONDS).join();
        //5.将封装好的信息加入索引库当中
        albumInfoIndexRepository.save(albumInfoIndex);
        //6.上架成功以后将专辑搜索信息加入到索引库中
        this.addSuggestInfo(albumId.toString(), albumInfoIndex.getAlbumTitle());
        //7.将通过审核的专辑id加入到布隆过滤器防止缓存穿透
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        if (bloomFilter.isExists()) {
            bloomFilter.add(albumId);
        }

    }

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 根据id下架相关专辑
     *
     * @param albumId
     */
    @Override
    public void lowerAlbum(Long albumId) {
        //1.根据id下架专辑
        albumInfoIndexRepository.deleteById(albumId);
        //2.根据id删除专辑的搜索信息
        suggestIndexRepository.deleteById(albumId.toString());

    }

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    /**
     * 查询es中已经上架的专辑信息
     *
     * @param albumIndexQuery
     * @return
     */
    @Override
    public AlbumSearchResponseVo getAlbumInfo(AlbumIndexQuery albumIndexQuery) {
        try {
            SearchRequest searchRequest = this.buildDls(albumIndexQuery);

            SearchResponse<AlbumInfoIndex> search = elasticsearchClient.search(searchRequest, AlbumInfoIndex.class);
            AlbumSearchResponseVo albumSearchResponseVo = this.buildResult(search, albumIndexQuery);
            return albumSearchResponseVo;
        } catch (IOException e) {
            throw new GuiguException(500, "查询es索引库失败");
        }


    }

    /**
     * 解析查询结果
     *
     * @param searchResponse
     * @param albumIndexQuery
     * @return
     */
    @Override
    public AlbumSearchResponseVo buildResult(SearchResponse<AlbumInfoIndex> searchResponse, AlbumIndexQuery albumIndexQuery) {
        AlbumSearchResponseVo albumSearchResponseVo = new AlbumSearchResponseVo();
        Integer pageNo = albumIndexQuery.getPageNo();
        Integer pageSize = albumIndexQuery.getPageSize();
        albumSearchResponseVo.setPageNo(pageNo);
        albumIndexQuery.setPageSize(pageSize);

        HitsMetadata<AlbumInfoIndex> hits = searchResponse.hits();
        long total = hits.total().value();

        //计算总页数
        long totalPageSize = total % pageSize == 0 ? total / pageSize : (total / pageSize) + 1;

        List<Hit<AlbumInfoIndex>> hitList = hits.hits();
        if (CollectionUtil.isNotEmpty(hitList)) {
            List<AlbumInfoIndexVo> list = hitList.stream().map(h -> {
                AlbumInfoIndex source = h.source();
                Map<String, List<String>> highlight = h.highlight();
                if (CollectionUtil.isNotEmpty(highlight)) {
                    String albumTitleHighLight = highlight.get("albumTitle").get(0);
                    source.setAlbumTitle(albumTitleHighLight);
                }
                return BeanUtil.copyProperties(source, AlbumInfoIndexVo.class);
            }).collect(Collectors.toList());
            albumSearchResponseVo.setList(list);
        }
        return albumSearchResponseVo;
    }


    private static final String INDEX_NAME = "albuminfo";

    @Override
    public SearchRequest buildDls(AlbumIndexQuery albumIndexQuery) {
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder.index(INDEX_NAME);
        Integer from = (albumIndexQuery.getPageNo() - 1) * albumIndexQuery.getPageSize();
        //1.组装分页条件
        builder.from(from);
        builder.size(albumIndexQuery.getPageSize());
        //2.组装高亮显示
        String keyword = albumIndexQuery.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            builder.highlight(h -> h.fields("albumTitle", p -> p.preTags("<font style='color:red'>").postTags("</font>")));
        }
        //3.组装全文检索
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        if (StringUtils.isNotBlank(keyword)) {
            boolBuilder.must(m -> m.match(m1 -> m1.field("albumTitle").query(albumIndexQuery.getKeyword())));
        }
        Long category1Id = albumIndexQuery.getCategory1Id();
        Long category2Id = albumIndexQuery.getCategory2Id();
        Long category3Id = albumIndexQuery.getCategory3Id();
        if (category1Id != null) {
            boolBuilder.filter(f -> f.term(t -> t.field("category1Id").value(category1Id)));
        }
        if (category2Id != null) {
            boolBuilder.filter(f -> f.term(t -> t.field("category2Id").value(category2Id)));
        }
        if (category2Id != null) {
            boolBuilder.filter(f -> f.term(t -> t.field("category3Id").value(category3Id)));
        }
        //拼装nested 属性（属性id:属性值id）
        List<String> attributeList = albumIndexQuery.getAttributeList();
        if (CollectionUtil.isNotEmpty(attributeList)) {
            for (String attribute : attributeList) {
                String[] split = attribute.split(":");
                if (split != null && split.length == 2) {
                    boolBuilder.filter(f -> f.nested(n -> n.path("attributeValueIndexList").query(q -> q.bool(b -> b.filter(bf -> bf.term(t -> t.field("attributeValueIndexList.attributeId").value(split[0]))).filter(f1 -> f1.term(t1 -> t1.field("attributeValueIndexList.valueId").value(split[1])))))));
                }
            }
        }
        builder.query(boolBuilder.build()._toQuery());

        //4.组装排序排序（综合排序[1:desc] 播放量[2:desc] 发布时间[3:desc]；asc:升序 desc:降序）")
        String order = albumIndexQuery.getOrder();
        if (StringUtils.isNotBlank(order)) {
            String[] split = order.split(":");
            if (split != null && split.length == 2) {
                String orderField = "";
                String orderNum = split[0];
                switch (orderNum) {
                    case "1":
                        orderField = "hotScore";
                        break;
                    case "2":
                        orderField = "playStatNum";
                        break;
                    case "3":
                        orderField = "createTime";
                        break;
                }
                String finalOrderField = orderField;
                SortOrder sortOrder = "asc".equals(split[1]) ? SortOrder.Asc : SortOrder.Desc;
                builder.sort(s -> s.field(f -> f.field(finalOrderField).order(sortOrder)));
            }
        }
        builder.source(s -> s.filter(f -> f.excludes("hotScore", "commentStatNum", "buyStatNum", "subscribeStatNum", "announcerName")));


        return builder.build();
    }

    /**
     * 根据分类1id获取热度前6的专辑信息
     *
     * @param category1Id
     * @return
     */
    @Override
    public List<Map<String, Object>> getTop6Album(Long category1Id) {
        try {
            //1.获取置顶的7个3级分类
            List<BaseCategory3> category3List = albumFeignClient.findTopBaseCategory3(category1Id).getData();
            //2.获取分类3列表当中的所有分类id
            List<FieldValue> fieldValueList = category3List.stream().map(m -> {
                return FieldValue.of(m.getId());
            }).collect(Collectors.toList());

            Map<Long, BaseCategory3> baseCategory3Map = category3List.stream().collect(Collectors.toMap(BaseCategory3::getId, b3 -> b3));

            //3.调用原生的esapi构建查询语句
            SearchRequest.Builder builder = new SearchRequest.Builder();
            builder.index(INDEX_NAME);
            builder.query(q -> q.bool(b -> b.filter(f -> f.terms(t -> t.field("category3Id").terms(t1 -> t1.value(fieldValueList)))))).size(0).aggregations("category3_agg", a -> a.terms(t -> t.field("category3Id").size(10)).aggregations("top6_agg", a1 -> a1.topHits(t -> t.size(6).sort(s -> s.field(f -> f.field("hotScore").order(SortOrder.Desc))))));
            SearchRequest build = builder.build();
            SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(build, AlbumInfoIndex.class);

            System.out.println(build);

            //4.获取查询结果
            List<LongTermsBucket> category3Agg = response.aggregations().get("category3_agg").lterms().buckets().array();
            if (CollectionUtil.isNotEmpty(category3Agg)) {
                List<Map<String, Object>> result = category3Agg.stream().map(bucket -> {
                    Map<String, Object> map = new HashMap<>();
                    //获取分类3id
                    long category3Id = bucket.key();
                    List<Hit<JsonData>> top6Agg = bucket.aggregations().get("top6_agg").topHits().hits().hits();
                    if (CollectionUtil.isNotEmpty(top6Agg)) {
                        List<AlbumInfoIndex> albumInfoIndexList = top6Agg.stream().map(hit -> {
                            String sourceStr = hit.source().toString();
                            AlbumInfoIndex albumInfoIndex = JSON.parseObject(sourceStr, AlbumInfoIndex.class);
                            return albumInfoIndex;
                        }).collect(Collectors.toList());
                        BaseCategory3 baseCategory3 = baseCategory3Map.get(category3Id);
                        map.put("baseCategory3", baseCategory3);
                        map.put("list", albumInfoIndexList);
                        return map;
                    }
                    return null;
                }).collect(Collectors.toList());
                return result;
            }
            return null;
        } catch (IOException e) {
            throw new GuiguException(500, "检索es失败");
        }
    }

    /**
     * 添加专辑搜索信息到索引库中
     *
     * @param id
     * @param title
     */
    @Override
    public void addSuggestInfo(String id, String title) {
        SuggestIndex suggestIndex = new SuggestIndex();
        suggestIndex.setId(id);
        suggestIndex.setTitle(title);
        suggestIndex.setKeyword(new Completion(new String[]{title}));
        String pinyin = PinyinUtil.getPinyin(title, "");
        String firstLetter = PinyinUtil.getFirstLetter(title, "");
        suggestIndex.setKeywordPinyin(new Completion(new String[]{pinyin}));
        suggestIndex.setKeywordSequence(new Completion(new String[]{firstLetter}));

        suggestIndexRepository.save(suggestIndex);
    }

    private static final String SUGGEST_INDEX_NAME = "suggestinfo";

    /**
     * 根据输入的关键字获取关键字自动补全
     *
     * @param keyword
     * @return
     */
    @Override
    public List<String> completeSuggest(String keyword) {
        try {
            HashSet<String> set = new HashSet<>();
            SearchRequest.Builder builder = new SearchRequest.Builder();
            builder.index(SUGGEST_INDEX_NAME);
            builder.suggest(s -> s.suggesters("suggest_keyword", s1 -> s1.prefix(keyword).completion(c -> c.field("keyword")))
                    .suggesters("suggest_pinyin", s1 -> s1.prefix(keyword).completion(c -> c.field("keywordPinyin")))
                    .suggesters("suggest_letter", s1 -> s1.prefix(keyword).completion(c -> c.field("keywordSequence")))
            );
            SearchRequest searchRequest = builder.build();
            SearchResponse<SuggestIndex> response = elasticsearchClient.search(searchRequest, SuggestIndex.class);
            //3.解析结果
            set.addAll(this.parseSuggestResult(response, "suggest_keyword"));
            set.addAll(this.parseSuggestResult(response, "suggest_pinyin"));
            set.addAll(this.parseSuggestResult(response, "suggest_letter"));
            if (set.size() < 10) {
                //根据用户输入的信息进行全文检索
                SearchResponse<AlbumInfoIndex> matchResponse = elasticsearchClient.search(
                        s -> s.index(INDEX_NAME).query(q -> q.match(m -> m.field("albumTitle")
                                .query(keyword))), AlbumInfoIndex.class);
                List<Hit<AlbumInfoIndex>> hits = matchResponse.hits().hits();
                if (CollectionUtil.isNotEmpty(hits)) {
                    for (Hit<AlbumInfoIndex> hit : hits) {
                        AlbumInfoIndex albumInfoIndex = hit.source();
                        set.add(albumInfoIndex.getAlbumTitle());
                        if (set.size() >= 10) {
                            break;
                        }
                    }
                }
            }
            if (set.size() < 10) {
                return new ArrayList<>(set);
            } else {
                return new ArrayList<>(set).subList(0, 9);
            }
        } catch (IOException e) {
            throw new GuiguException(500, "索引库获取关键字补全失败");
        }
    }

    /**
     * 解析suggest检索结果
     *
     * @param response
     * @param suggestName
     * @return
     */
    @Override
    public List<String> parseSuggestResult(SearchResponse<SuggestIndex> response, String suggestName) {
        List<String> list = new ArrayList<>();

        List<Suggestion<SuggestIndex>> suggestions = response.suggest().get(suggestName);
        if (CollectionUtil.isNotEmpty(suggestions)) {
            for (Suggestion<SuggestIndex> suggestion : suggestions) {
                List<CompletionSuggestOption<SuggestIndex>> options = suggestion.completion().options();
                for (CompletionSuggestOption<SuggestIndex> option : options) {
                    String title = option.source().getTitle();
                    list.add(title);
                }
            }
        }
        return list;
    }

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据一级分类id和排行标识获取到排行榜 从redis中获取数据
     *
     * @param category1Id
     * @param dimension
     * @return
     */
    @Override
    public List<AlbumInfoIndexVo> findRankingList(Long category1Id, String dimension) {
        //1.构建redis的key
        String redisKey = RedisConstant.RANKING_KEY_PREFIX + category1Id;
        //2.检查该key是否在redis中存在
        Boolean flag = redisTemplate.hasKey(redisKey);
        if (flag) {
            List<AlbumInfoIndex> list = (List<AlbumInfoIndex>) redisTemplate.opsForHash().get(redisKey, dimension);
            if (CollectionUtil.isNotEmpty(list)) {
                List<AlbumInfoIndexVo> albumInfoIndexVoList = list.stream().map(albumInfoIndex -> {
                            AlbumInfoIndexVo albumInfoIndexVo = BeanUtil.copyProperties(albumInfoIndex, AlbumInfoIndexVo.class);
                            return albumInfoIndexVo;
                        }
                ).collect(Collectors.toList());
                return albumInfoIndexVoList;
            }
        }
        return null;
    }


    /**
     * 更新所有分类下的排行榜，存入redis中
     */
    @Override
    public void updateLatelyAlbumRanking() {
        //1.获取所有一级分类的id
        List<BaseCategory1> baseCategory1List = albumFeignClient.findAllCategory1().getData();
        //2.获取所有一级分类的id
        List<Long> category1IdList = baseCategory1List.stream().map(baseCategory1 -> baseCategory1.getId()).collect(Collectors.toList());
        for (Long category1Id : category1IdList) {
            String redisKey = RedisConstant.RANKING_KEY_PREFIX + category1Id;
            //3.构建分类下的查询类型的数组
            String[] rankingDimensionArray =
                    new String[]{"hotScore", "playStatNum", "subscribeStatNum", "buyStatNum", "commentStatNum"};
            for (String demision : rankingDimensionArray) {
                //4.构建redisKey
                try {
                    //5.从es中检索根据条件检索排行前20的数据
                    SearchResponse<AlbumInfoIndex> response = elasticsearchClient.
                            search(s -> s.index(INDEX_NAME)
                                            .query(t -> t.term(t1 -> t1.field("category1Id")
                                                    .value(category1Id))).size(20)
                                            .sort(s1 -> s1.field(s2 -> s2.field(demision).order(SortOrder.Desc)))
                                    , AlbumInfoIndex.class);
                    //6.解析检索出的结果
                    List<Hit<AlbumInfoIndex>> hits = response.hits().hits();
                    if (CollectionUtil.isNotEmpty(hits)) {
                        List<AlbumInfoIndex> albumInfoIndexList = hits.stream().map(hit -> hit.source()).collect(Collectors.toList());
                        //7.将检索出的结果存入redis中
                        redisTemplate.opsForHash().put(redisKey, demision, albumInfoIndexList);
                    }
                } catch (IOException e) {
                    throw new GuiguException(500, "条件检索索引库中排行前20的数据失败");
                }
            }

        }

    }


}
