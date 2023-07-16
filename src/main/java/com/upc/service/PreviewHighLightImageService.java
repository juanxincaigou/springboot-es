package com.upc.service;

import com.upc.pojo.ImagePosition;

import java.io.File;

public interface PreviewHighLightImageService {
    byte[] previewHighLightImage(ImagePosition imagePosition, File file, String fileName, int pageNumber);
}
