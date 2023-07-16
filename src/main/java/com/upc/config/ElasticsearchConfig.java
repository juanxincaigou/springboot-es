package com.upc.config;

import org.apache.http.HttpHost;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

//#elasticsearch可以在这里配置连接，也可以在配置文件中配置
@Configuration
public class ElasticsearchConfig {
//    同步客户端
//    @Bean
//    public RestHighLevelClient restHighLevelClient(){
//        RestClientBuilder builder = RestClient.builder(
//                new HttpHost("127.0.0.1", 9200, "http"));
//        return new RestHighLevelClient(builder);
//    }

    //配置异步客户端
    private static final int maxConnTotal = 100; //连接池的最大连接数
    private static final int maxConnPerRoute = 50;
    //设置每个路由的最大连接数，即每个目标主机的最大连接数
    @Bean
    public RestHighLevelClient restHighLevelClient() throws IOException {

        RestClientBuilder restClientBuilder = RestClient.builder(
                new HttpHost("127.0.0.1", 9200, "http")
        ) .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                return httpClientBuilder
                        .setMaxConnTotal(maxConnTotal)
                        .setMaxConnPerRoute(maxConnPerRoute);
            }
        })
                .setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
                    @Override
                    public org.apache.http.client.config.RequestConfig.Builder customizeRequestConfig(
                            org.apache.http.client.config.RequestConfig.Builder requestConfigBuilder) {
                        return requestConfigBuilder.setConnectTimeout(5000)
                                .setSocketTimeout(60000);
                    }
                });

        return new RestHighLevelClient(restClientBuilder);

//          简练版
//        RestClientBuilder restClientBuilder = RestClient.builder(
//                new HttpHost("127.0.0.1", 9200, "http")
//        ) .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
//                .setMaxConnTotal(maxConnTotal)
//                .setMaxConnPerRoute(maxConnPerRoute))
//                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(5000)
//                        .setSocketTimeout(60000));
//        return new RestHighLevelClient(restClientBuilder);
    }
}

