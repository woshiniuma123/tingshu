package com.atguigu.tingshu;

import com.atguigu.tingshu.search.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ServiceSearchApplication.class)
public class BatchImportTest {

    @Autowired
    private SearchService searchService;


    /**
     * 采用for循环导入专辑，不严谨导入，专辑ID如果存在“断层”查询专辑
     */
    @Test
    public void test() {
        for (long i = 1; i <= 1634; i++) {
            try {
                searchService.upperAlbum(i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        searchService.upperAlbum(1603l);
    }


}