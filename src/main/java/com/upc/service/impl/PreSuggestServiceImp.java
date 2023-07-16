package com.upc.service.impl;

import com.upc.service.PreSuggestService;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PreSuggestServiceImp implements PreSuggestService {
    @Resource
    private RestHighLevelClient client;

    @Override
    public List<String> prefixSuggest(String prefix) throws IOException {
        String index = "article";
        String field = "titleSuggest";
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        SuggestBuilder suggestBuilder = new SuggestBuilder();
        CompletionSuggestionBuilder completionSuggestionBuilder =
                SuggestBuilders.completionSuggestion(field)
                        .prefix(prefix)
                        .size(10)
                        .skipDuplicates(true);

        suggestBuilder.addSuggestion("suggest", completionSuggestionBuilder);
        sourceBuilder.suggest(suggestBuilder);

        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.source(sourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        Suggest suggest = searchResponse.getSuggest();
        CompletionSuggestion completionSuggestion = suggest.getSuggestion("suggest");
        List<CompletionSuggestion.Entry.Option> options = completionSuggestion.getOptions();
        List<String> result = options.stream()
                .map(option -> option.getText().toString())
                .collect(Collectors.toList());
        return result;
    }
}
