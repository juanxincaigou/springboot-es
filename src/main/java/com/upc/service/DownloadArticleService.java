package com.upc.service;

import org.springframework.core.io.InputStreamResource;

import java.io.File;
import java.io.FileNotFoundException;

public interface DownloadArticleService {
    InputStreamResource downloadArticle(File file,String fileName) throws FileNotFoundException;
}
