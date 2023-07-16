package com.upc.service.impl;

import com.upc.service.PreviewArticleService;
import com.upc.utils.PDDocumentPool;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.File;

@Service
public class PreviewArticleServiceImp implements PreviewArticleService {

    @Resource
    private PDDocumentPool documentPool;

    @Override
    public byte[] previewArticle(File file) throws Exception {
        // 读取 PDF 文件内容
        PDDocument document = documentPool.borrowObject().load(file);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.save(outputStream);
        return outputStream.toByteArray();
    }
}
