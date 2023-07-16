package com.upc.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface IndexArticleService {
    void indexArticle(String indexName, List<File> files) throws IOException;
}
