package com.upc.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface UploadArticleService {
    List<File> uploadArticle (File uploadDir,MultipartFile[] files) throws IOException;
}
