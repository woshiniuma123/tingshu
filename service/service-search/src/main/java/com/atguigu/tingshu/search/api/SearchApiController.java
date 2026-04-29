package com.atguigu.tingshu.search.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "搜索专辑管理")
@RestController
@RequestMapping("api/search")
@SuppressWarnings({"all"})
public class SearchApiController {

    @Autowired
    private SearchService searchService;


    @GetMapping("/albumInfo/upperAlbum/{albumId}")
    public Result upperAlbum(@PathVariable Long albumId) {
        searchService.upperAlbum(albumId);
        return Result.ok();
    }

    @GetMapping("/albumInfo/lowerAlbum/{albumId}")
    public Result lowerAlbum(@PathVariable Long albumId) {
        searchService.lowerAlbum(albumId);
        return Result.ok();
    }

    @Operation(summary = "查询es库中已经上架的专辑信息")
    @PostMapping("/albumInfo")
    public Result<AlbumSearchResponseVo> getAlbumInfo(@RequestBody AlbumIndexQuery albumIndexQuery) {
        AlbumSearchResponseVo albumSearchResponseVo = searchService.getAlbumInfo(albumIndexQuery);
        return Result.ok(albumSearchResponseVo);
    }

    @GetMapping("/albumInfo/channel/{category1Id}")
    public Result<List<Map<String, Object>>> getTop6Album(@PathVariable Long category1Id) {
        List<Map<String, Object>> result = searchService.getTop6Album(category1Id);
        return Result.ok(result);
    }

    @GetMapping("/albumInfo/completeSuggest/{keyword}")
    public Result<List<String>> completeSuggest(@PathVariable String keyword) {
        List<String> list = searchService.completeSuggest(keyword);
        return Result.ok(list);
    }

    @Operation(summary = "根据一级分类和查询标识获取专辑排行榜")
    @GetMapping("/albumInfo/findRankingList/{category1Id}/{dimension}")
    public Result<List<AlbumInfoIndexVo>> findRankingList(@PathVariable Long category1Id, @PathVariable String dimension) {
        List<AlbumInfoIndexVo> albumInfoIndexList = searchService.findRankingList(category1Id, dimension);
        return Result.ok(albumInfoIndexList);
    }

    /**
     * 更新所有分类下的所有标识的排行榜
     *
     * @return
     */
    @Operation(summary = "更新所有分类下的排行榜")
    @GetMapping("/albumInfo/updateLatelyAlbumRanking")
    public Result updateLatelyAlbumRanking() {
        searchService.updateLatelyAlbumRanking();
        return Result.ok();
    }
}

