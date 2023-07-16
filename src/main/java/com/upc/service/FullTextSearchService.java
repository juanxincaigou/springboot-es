package com.upc.service;


import java.io.IOException;
import java.util.Map;

public interface FullTextSearchService {
    Map<String, Object> fullTextSearch(String keyword) throws IOException;
}
