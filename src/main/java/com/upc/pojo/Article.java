package com.upc.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.completion.Completion;
import org.springframework.stereotype.Component;

//设置es表名
//createIndex = false 设置不自动创建index
@Component
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "article",createIndex = false)
//@Setting(settingPath = "setting/setting.json")
public class Article {

    //analyzer：插入文档时，将text类型的字段做分词然后插入倒排索引。
    //searchAnalyzer：查询时，先对要查询的text类型的输入做分词，再去倒排索引中搜索
    //text：存储数据时候，会自动分词，并生成索引
    //keyword：存储数据时候，不会分词建立索引
    //index：是否索引，布尔类型，默认是true
    //store：是否存储，布尔类型，默认是false

    @Id
    private String id;

    private String title;

    private Completion titleSuggest;

    private String type;

    private Page page;

    private String fileUrl;

    private String updateTime;

    private String author;

}
