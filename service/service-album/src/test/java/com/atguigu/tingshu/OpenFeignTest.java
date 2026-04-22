package com.atguigu.tingshu;

import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class OpenFeignTest {

    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;


    @Test
    public void test() {

        Result<AlbumInfo> albumInfo = albumFeignClient.getAlbumInfo(1L);
        System.out.println(albumInfo.getData());
        Long id = albumInfo.getData().getId();
        System.out.println(id);

        BaseCategoryView data = albumFeignClient.getCategoryViewByCategory3Id(1017L).getData();
        System.out.println(data);

        UserInfoVo userInfo = userFeignClient.getUserInfo(37L).getData();
        System.out.println(userInfo);
    }
}
