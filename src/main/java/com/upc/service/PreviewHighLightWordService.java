package com.upc.service;

import java.io.File;

public interface PreviewHighLightWordService {
    byte[] previewHighLightWord(String keyWord, File file, String fileName, int pageNumber);
}
