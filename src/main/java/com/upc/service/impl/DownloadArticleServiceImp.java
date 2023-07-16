package com.upc.service.impl;

import com.upc.service.DownloadArticleService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

@Service
public class DownloadArticleServiceImp implements DownloadArticleService {
    @Override
    public InputStreamResource downloadArticle(File file,String fileName) throws FileNotFoundException {
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        return resource;
    }
}
