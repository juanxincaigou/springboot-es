package com.upc.service.impl;

import com.upc.service.SearchByAuthorService;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class SearchByAuthorServiceImp implements SearchByAuthorService {

    @Resource
    private RestHighLevelClient client;

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Map<String, Object> searchByAuthor(String author) {
        String indexName = "article";
        String fieldName  = "author";
        QueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery(fieldName, author))
                .must(QueryBuilders.matchQuery("title", author));

        HighlightBuilder highlightBuilder = new HighlightBuilder()
                .field("title")
                .field(fieldName);

        highlightBuilder.preTags("<span style=\"color:red\">");      //高亮设置
        highlightBuilder.postTags("</span>");


        highlightBuilder.numOfFragments(0);                             //从第一个分片获取高亮片段

        int pageNum = 1; // 指定当前页码
        int pageSize = 10000; // 指定每页显示数量
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(queryBuilder)
                .highlighter(highlightBuilder)
                .from((pageNum - 1) * pageSize)
                .size(pageSize)
                .sort(SortBuilders.scoreSort().order(SortOrder.DESC))
                //        SortBuilders.fieldSort("_score").order(SortOrder.DESC)        根据指定字段排序
                //指定要获取的字段,第一个参数为包含的字段，第二个参数为不包含的字段
                .fetchSource(new String[]{}, null);


        SearchRequest searchRequest = new SearchRequest()
                .indices(indexName)
                .source(searchSourceBuilder);


        // 发送搜索请求并获取响应结果
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

//         处理查询结果
        //获取命中的文档
        SearchHits hits = searchResponse.getHits();
        //最后需要返回的map
        Map<String, Object> resultMap = new HashMap<>();
        HashSet<Map<String, Object>> articleIdAndTitleAndUrlAndAuthorSet = new HashSet<>();

        List<Map<String, Object>> articleList = new ArrayList<>();  //存每个pdf

        //使用HashSet过滤出每个有命中页的pdf
        for (SearchHit hit : hits.getHits()) {
            Map<String, Object> idAndTitleAndUrlAndAuthor = new HashMap<>();
            idAndTitleAndUrlAndAuthor.put("articleId", StringUtils.substringBeforeLast(hit.getId(), "-"));
            Map<String, Object> hitPage = hit.getSourceAsMap();
            idAndTitleAndUrlAndAuthor.put("fileUrl", StringUtils.replace((String) hitPage.get("fileUrl"), "\\", "/"));
            //题目中命中关键字，替换为高亮的
            if (hit.getHighlightFields() != null && !hit.getHighlightFields().isEmpty()) {
                if (hit.getHighlightFields().get("title") != null) {
                    idAndTitleAndUrlAndAuthor.put("articleTitle",hit.getHighlightFields().get("title").fragments()[0].string());
                }
                if (hit.getHighlightFields().get(fieldName) != null) {
                    idAndTitleAndUrlAndAuthor.put("author",hit.getHighlightFields().get(fieldName).fragments()[0].string());
                }
            }
            articleIdAndTitleAndUrlAndAuthorSet.add(idAndTitleAndUrlAndAuthor);
        }

        //命中的pdf数
        resultMap.put("totalHits",articleIdAndTitleAndUrlAndAuthorSet.size());

        //构造出最后要返回给前端的json格式的大体结构
        for (Map<String, Object> idAndTitleAndUrlAndAuthor : articleIdAndTitleAndUrlAndAuthorSet){
            Map<String, Object> article = new HashMap<>();
            article.put("articleId", idAndTitleAndUrlAndAuthor.get("articleId"));
            article.put("articleTitle",idAndTitleAndUrlAndAuthor.get("articleTitle"));
            article.put("fileUrl",idAndTitleAndUrlAndAuthor.get("fileUrl"));
            article.put("author",idAndTitleAndUrlAndAuthor.get("author"));
            articleList.add(article);
            resultMap.put("articleList",articleList);
        }
        return resultMap;
    }
}
