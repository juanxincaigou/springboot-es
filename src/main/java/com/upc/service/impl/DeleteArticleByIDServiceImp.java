package com.upc.service.impl;

import com.upc.service.DeleteArticleByIDService;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.PrefixQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

@Service
public class DeleteArticleByIDServiceImp implements DeleteArticleByIDService {
    @Resource
    private RestHighLevelClient client;
    @Override
    public int deleteArticleByID(File deleteDir,String fileName) throws IOException {
        String id = StringUtils.substringBeforeLast(fileName,"--");
        PrefixQueryBuilder prefixQueryBuilder = QueryBuilders.prefixQuery("id", id);
        int pageNum = 1; // 指定当前页码
        int pageSize = 10000; // 指定每页显示数量
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(prefixQueryBuilder)
                .from((pageNum - 1) * pageSize)
                .size(pageSize)
                .fetchSource(new String[]{"id"}, null);
        SearchRequest searchRequest = new SearchRequest()
                .indices("article")
                .source(searchSourceBuilder);

        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        SearchHits hits = searchResponse.getHits();
        BulkRequest bulkRequest = new BulkRequest();
        int totalPage = hits.getHits().length;
        System.out.println(totalPage);
        for (SearchHit hit : hits) {
            System.out.println(hit.getId());
            DeleteRequest deleteRequest = new DeleteRequest("article").id(hit.getId());
            bulkRequest.add(deleteRequest);
        }
        client.bulk(bulkRequest, RequestOptions.DEFAULT);
        fileName =  StringUtils.contains(fileName,".pdf") ? fileName : fileName + ".pdf";
        File pdfPath = new File(deleteDir + "/" +fileName);
//        System.out.println(pdfPath.getPath());
        pdfPath.delete();
        return totalPage;
    }
}
