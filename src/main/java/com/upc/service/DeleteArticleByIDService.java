package com.upc.service;

import java.io.File;
import java.io.IOException;

public interface DeleteArticleByIDService {
    int deleteArticleByID(File deleteDir, String id) throws IOException;
}
