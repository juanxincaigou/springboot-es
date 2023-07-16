package com.upc.service.impl;


import com.upc.service.FullTextSearchService;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;


@Service
public class FullTextSearchServiceImp implements FullTextSearchService {

//    @Resource
//    private ElasticsearchRestTemplate elasticsearchRestTemplate;
//
//    @Resource
//    private ArticleRepository articleRepository;

    @Resource
    private RestHighLevelClient client;


    @Override
    public Map<String, Object> fullTextSearch(String keyWord){
        String indexName = "article";
        String nestedField = "page";
        String fieldName  = "title";
        QueryBuilder queryBuilder = QueryBuilders.boolQuery()
//                .should(QueryBuilders.matchQuery(fieldName,keyWord))
                .should(QueryBuilders.nestedQuery(nestedField,
                        QueryBuilders.matchQuery(nestedField + ".content", keyWord),
                        ScoreMode.Max))
                .should(QueryBuilders.nestedQuery(nestedField,
                        QueryBuilders.matchQuery(nestedField + ".pageImagesContents", keyWord),
                        ScoreMode.Max));
        //将嵌套文档得分的平均值作为父文档得分

        HighlightBuilder highlightBuilder = new HighlightBuilder()
                .field(fieldName)
                .field(nestedField + ".pageImagesContents")
                .field(nestedField + ".content");

        highlightBuilder.requireFieldMatch(false);                      //如果要多个字段高亮,这项要为false
        highlightBuilder.preTags("<span style=\"color:red\">");      //高亮设置
        highlightBuilder.postTags("</span>");

        //要高亮如文字内容等有很多字的字段,必须配置,不然会导致高亮不全,文章内容缺失等
        highlightBuilder.fragmentSize(800000);                          //最大高亮分片数
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
                .fetchSource(new String[]{fieldName,nestedField + ".pageNumber",nestedField + ".pageImagesContents","fileUrl",nestedField+".imgPositions"}, null);


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
        HashSet<Map<String, Object>> articleIdAndTitleAndUrlSet = new HashSet<>();

        List<Map<String, Object>> articleList = new ArrayList<>();  //存每个pdf

        //使用HashSet过滤出每个有命中页的pdf
        for (SearchHit hit : hits.getHits()) {
            Map<String, Object> idAndTitleAndUrl = new HashMap<>();
            idAndTitleAndUrl.put("articleId",StringUtils.substringBeforeLast(hit.getId(), "-"));
            //普通title
            Map<String, Object> hitPage = hit.getSourceAsMap();
            idAndTitleAndUrl.put("articleTitle",hitPage.get("title"));
            idAndTitleAndUrl.put("fileUrl",StringUtils.replace((String) hitPage.get("fileUrl"), "\\", "/"));
            //如果pdf题目中命中关键字，替换为高亮的
            if (hit.getHighlightFields() != null && !hit.getHighlightFields().isEmpty()) {
                if (hit.getHighlightFields().get("title") != null) {
                    idAndTitleAndUrl.put("articleTitle",hit.getHighlightFields().get("title").fragments()[0].string());
                }
            }
            articleIdAndTitleAndUrlSet.add(idAndTitleAndUrl);
        }

        //命中的pdf数
        resultMap.put("totalHits",articleIdAndTitleAndUrlSet.size());

        //构造出最后要返回给前端的json格式的大体结构
        for (Map<String, Object> idAndTitleAndUrl : articleIdAndTitleAndUrlSet){
            Map<String, Object> article = new HashMap<>();
            article.put("articleId", idAndTitleAndUrl.get("articleId"));
            article.put("articleTitle",idAndTitleAndUrl.get("articleTitle"));
            article.put("fileUrl",idAndTitleAndUrl.get("fileUrl"));
            article.put("pageList",new ArrayList<>());
            articleList.add(article);
            resultMap.put("articleList",articleList);
        }

        //存每个pdf中的页
        List<Map<String, Object>> pageList ;
        for (SearchHit hit : hits.getHits()) {
            String articleId = StringUtils.substringBeforeLast(hit.getId(), "-");
            for (Map<String, Object> article : articleList){
                if (articleId.equals(article.get("articleId").toString())){
                    //获取单页源数据Map对象
                    Map<String, Object> hitPage = hit.getSourceAsMap();
                    //添加每页得分
                    hitPage.put("pageScore",hit.getScore());
                    //替换路径中的‘\\’
                    hitPage.put("fileUrl",StringUtils.replace((String) hitPage.get("fileUrl"), "\\", "/"));
                    //页数，本来是个hashmap{pageNumber=9}这种，利用键名一致会自动替换值，换成数字
                    HashMap<String, Object> page = (HashMap<String, Object>) hitPage.get("page");
                    hitPage.put("page",page.get("pageNumber"));
                    hitPage.put("imgPositions",page.get("imgPositions"));
                    int hitCount = 0;
                    hitPage.put("hitCount", hitCount);
                    // 处理高亮显示结果
                    //获取命中文档中所有高亮字段和对应的高亮结果Map对象
                    Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                    if (highlightFields != null && !highlightFields.isEmpty()) {
                        //从高亮结果中获取特定字段的高亮结果
                        HighlightField highlightNestedField = highlightFields.get(nestedField + ".content");
                        //如果存在高亮结果则将其替换原文本
                        if (highlightNestedField != null) {
                            hitPage.put("content", highlightNestedField.fragments()[0].string());
                            //获取到高亮片段，然后统计高亮字段对应内容中完全命中关键词的次数
                            Text[] fragments = highlightNestedField.getFragments();
                            if (fragments != null && fragments.length > 0) {
                                for (Text fragment : fragments) {
                                    hitCount += StringUtils.countMatches(fragment.string(), keyWord);
                                }
                                hitPage.put("hitCount", hitCount);
                            }
                        }
                        HighlightField highlightImageContentsField = highlightFields.get(nestedField + ".pageImagesContents");
                        if (highlightImageContentsField != null){
                            List<String> highlightImageContents = new ArrayList<>();
                            Text[] fragments = highlightImageContentsField.fragments();
                            for(Text text:fragments){
                                highlightImageContents.add(text.toString());
                            }
                            hitPage.put("pageImageContents",highlightImageContents);
                        }
                    }
                    pageList = (List<Map<String, Object>>)article.get("pageList");
                    pageList.add(hitPage);
                }
            }
        }
        for(Map<String, Object> article :articleList){
            int articleHitCount = 0;
            Double articleScore = 0.0;
            List<Map<String, Object>> list = (List<Map<String, Object>>)article.get("pageList");
            for (Map<String, Object> page :list){
                articleHitCount += Integer.parseInt(page.get("hitCount").toString());
                articleScore += Double.parseDouble(page.get("pageScore").toString());
            }
            article.put("articleScore",articleScore);
            article.put("articleHitCount",articleHitCount);
        }
        articleList.sort((article1,article2) -> {
            Double articleScore1 = Double.valueOf(article1.getOrDefault("articleScore",Double.MIN_VALUE).toString());
            Double articleScore2 = Double.valueOf(article2.getOrDefault("articleScore",Double.MIN_VALUE).toString());
            return articleScore2.compareTo(articleScore1); // 降序
        });

        return resultMap;
    }

}



