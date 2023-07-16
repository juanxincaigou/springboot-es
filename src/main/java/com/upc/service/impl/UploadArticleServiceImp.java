package com.upc.service.impl;

import com.upc.service.UploadArticleService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UploadArticleServiceImp implements UploadArticleService {
    @Override
    public List<File>  uploadArticle(File uploadDir,MultipartFile[] files) throws IOException {
        // 保存上传的文件，并记录文件名到列表中
        List<File> fileList = new ArrayList<>();
        // 创建上传目录（如果不存在）
        if (!uploadDir.exists()) {
            uploadDir.mkdir();
        }
        for (MultipartFile file : files) {
            String fileName = UUID.randomUUID() + "--" + file.getOriginalFilename();
            // 将MultipartFile转换为File,一定要使用绝对路径
            File dest = new File(uploadDir.getCanonicalFile() + "/" + fileName);
            file.transferTo(dest);
            fileList.add(dest);
        }
        return fileList;
    }
}
