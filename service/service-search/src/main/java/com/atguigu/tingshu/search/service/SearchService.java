package com.atguigu.tingshu.search.service;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.SuggestIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;

import java.util.List;
import java.util.Map;

public interface SearchService {


    void upperAlbum(Long albumId);

    void lowerAlbum(Long albumId);

    AlbumSearchResponseVo getAlbumInfo(AlbumIndexQuery albumIndexQuery);

    SearchRequest buildDls(AlbumIndexQuery albumIndexQuery);

    AlbumSearchResponseVo buildResult(SearchResponse<AlbumInfoIndex> searchResponse, AlbumIndexQuery albumIndexQuery);

    List<Map<String, Object>> getTop6Album(Long category1Id);

    void addSuggestInfo(String id, String title);

    List<String> completeSuggest(String keyword);

    List<String> parseSuggestResult(SearchResponse<SuggestIndex> response, String suggestName);

    List<AlbumInfoIndexVo> findRankingList(Long category1Id, String dimension);

    void updateLatelyAlbumRanking();

}
