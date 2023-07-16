package com.upc.service;

import java.io.File;

public interface PreviewArticleService {
    byte[] previewArticle(File file) throws Exception;
}
