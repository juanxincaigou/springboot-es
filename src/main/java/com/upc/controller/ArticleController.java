package com.upc.controller;

import com.upc.pojo.ImagePosition;
import com.upc.service.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

@Controller
public class ArticleController {

    @Resource
    private IndexArticleService indexArticleService;

    @Resource
    private  UploadArticleService UploadArticleService;

    @Resource
    private DownloadArticleService downloadArticleService;

    @Resource
    private PreviewArticleService previewArticleService;

    @Resource
    private  DeleteArticleByIDService deleteArticleByIDService;

    @Resource
    private PreviewHighLightImageService previewHighLightImageService;

    @Resource
    private PreviewHighLightWordService previewHighLightWordService;

    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/";

//    @Resource(name = "asyncExecutor")
//    private ThreadPoolTaskExecutor executor;


//    @ResponseBody
//    @PutMapping("/indexFile")
//    public String uploadArticle() throws IOException {
//        indexArticleService.indexArticle("article");
//        return "文件索引成功";
//    }

    @ResponseBody
    @PostMapping(value = "/upload")
    public ResponseEntity<String> uploadAndIndex(@RequestParam("files") MultipartFile[] files) {
        try {
            File uploadDir = new File(UPLOAD_DIR);
            List<File> fileList = UploadArticleService.uploadArticle(uploadDir,files);
            indexArticleService.indexArticle("article",fileList);
            return ResponseEntity.ok().body("Files uploaded successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload files: " + e.getMessage());
        }
    }



    //下载的时候要文件下载鉴权、防盗链
    // JWT身份验证和权限校验
    // token验证在文件下载链接中添加一个 token 参数，后端根据该参数进行验证，确保请求来自合法用户
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadPDF(@RequestParam("fileName") String fileName){
        File file = new File(UPLOAD_DIR + fileName);
        InputStreamResource resource = null;
        try {
            resource = downloadArticleService.downloadArticle(file,fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + file.getName());
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/pdf"))
                .body(resource);
    }

    @GetMapping("/previewPdf")
    public ResponseEntity<byte[]> previewPdfFile(@RequestParam("fileName") String fileName){
        byte[] fileContent = null;
        File file = new File(UPLOAD_DIR + fileName);
        try {
            fileContent = previewArticleService.previewArticle(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("inline").filename(file.getName()).build());
        // 返回响应实体对象
        return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
    }

    @PostMapping ("/previewHighLightImage")
    public ResponseEntity<byte[]> previewHighLightImage(@RequestParam("fileName") String fileName, @RequestParam("pageNumber") int pageNumber,@RequestBody ImagePosition imagePosition){
        byte[] fileContent = null;
        File file = new File(UPLOAD_DIR + fileName);
        try {
            fileContent = previewHighLightImageService.previewHighLightImage(imagePosition,file,fileName,pageNumber);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("inline").filename(file.getName()).build());
        // 返回响应实体对象
        return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
    }

    @PostMapping ("/previewHighLightWord")
    public ResponseEntity<byte[]> previewHighLightWord(@RequestParam("keyWord")String keyWord , @RequestParam("fileName") String fileName, @RequestParam("pageNumber") int pageNumber){
        byte[] fileContent = null;
        File file = new File(UPLOAD_DIR + fileName);
        try {
            fileContent = previewHighLightWordService.previewHighLightWord(keyWord,file,fileName,pageNumber);
        } catch (Exception e) {
        e.printStackTrace();
        }
        // 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("inline").filename(file.getName()).build());
        // 返回响应实体对象
        return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
    }
    @ResponseBody
    @GetMapping ("/deleteFile")
    public String deleteArticle(@RequestParam("fileName")String fileName) throws IOException {
        File deleteDir = new File(UPLOAD_DIR);
        int totalPage = deleteArticleByIDService.deleteArticleByID(deleteDir,fileName);
        return "文献删除成功,共" + totalPage + "页";
    }
}
