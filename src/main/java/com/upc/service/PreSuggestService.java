package com.upc.service;

import java.io.IOException;
import java.util.List;

public interface PreSuggestService {
    List<String> prefixSuggest(String prefix) throws IOException;
}
