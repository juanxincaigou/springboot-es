package com.upc.controller;


import com.alibaba.fastjson.JSONArray;
import com.upc.service.FullTextSearchService;
import com.upc.service.PreSuggestService;
import com.upc.service.SearchByAuthorService;
import com.upc.service.SearchByTitleLikeService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController {

    @Resource
    private FullTextSearchService fullTextSearchService;

    @Resource
    private PreSuggestService preSuggestService;

    @Resource
    private SearchByTitleLikeService searchByTitleLikeService;

    @Resource
    private SearchByAuthorService searchByAuthorService;

//    @Resource
//    private RestHighLevelClient client;

    @ResponseBody
    @GetMapping("/fullTextSearch")
    public void fullTextSearch(@RequestParam("keyword") String keyword,HttpServletResponse resp){
        try {
            Map<String, Object> articles = fullTextSearchService
                    .fullTextSearch(keyword);
            String data = JSONArray.toJSONString(articles);
            resp.setCharacterEncoding("utf-8");
            PrintWriter respWriter = resp.getWriter();
            respWriter.append(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @ResponseBody
    @GetMapping("/searchByTitleLike")
    public void searchByTitleLike(@RequestParam("keyword") String keyword,HttpServletResponse resp){
        try {
            Map<String, Object> articles = searchByTitleLikeService
                    .searchByTitleLike(keyword);
            String data = JSONArray.toJSONString(articles);
            resp.setCharacterEncoding("utf-8");
            PrintWriter respWriter = resp.getWriter();
            respWriter.append(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @ResponseBody
    @GetMapping("/searchByAuthor")
    public void searchByAuthor(@RequestParam("author") String author,HttpServletResponse resp){
        try {
            Map<String, Object> articles = searchByAuthorService
                    .searchByAuthor(author);
            String data = JSONArray.toJSONString(articles);
            resp.setCharacterEncoding("utf-8");
            PrintWriter respWriter = resp.getWriter();
            respWriter.append(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @ResponseBody
    @GetMapping("/prefixSuggest")
    public List<String> suggest(@RequestParam("prefix") String prefix) throws IOException {
        List<String> suggest = preSuggestService.prefixSuggest(prefix);
        return suggest;
    }


}
