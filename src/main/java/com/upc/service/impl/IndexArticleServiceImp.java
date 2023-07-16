package com.upc.service.impl;

import com.alibaba.fastjson.JSON;
import com.spire.pdf.PdfDocument;
import com.spire.pdf.PdfPageBase;
import com.spire.pdf.exporting.PdfImageInfo;
import com.upc.pojo.Article;
import com.upc.pojo.ImagePosition;
import com.upc.pojo.Page;
import com.upc.service.IndexArticleService;
import com.upc.utils.PDDocumentPool;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.data.elasticsearch.core.completion.Completion;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@Service
public class IndexArticleServiceImp implements IndexArticleService {
    @Resource
    private RestHighLevelClient client;

    @Resource
    private Article article;

    @Resource
    private PDDocumentPool documentPool;


    @Async("asyncExecutor")
    @Override
    public void indexArticle(String indexName, List<File> files) throws IOException {
        GetIndexRequest req = new GetIndexRequest(indexName);
        boolean exists = client.indices().exists(req, RequestOptions.DEFAULT);
        if (!exists){
            CreateIndexRequest request = new CreateIndexRequest(indexName);
            //自定义setting
            XContentBuilder settingBuilder = XContentFactory.jsonBuilder()
                    .startObject()
                    .startObject("analysis")
                    .startObject("analyzer")
                    .startObject("ik_smart_lowercase")
                    .field("type", "custom")
                    .field("tokenizer", "ik_smart")
                    .array("filter", "lowercase")
                    .endObject()
                    .startObject("ik_max_word_lowercase")
                    .field("type", "custom")
                    .field("tokenizer", "ik_max_word")
                    .array("filter", "lowercase")
                    .endObject()
                    .endObject()
                    .endObject()
                    .endObject();
            request.settings(settingBuilder);
            // 自定义 mapping
            XContentBuilder mappingBuilder = XContentFactory.jsonBuilder()
                    .startObject()
                    .startObject("properties")
                    .startObject("fileUrl")
                    .field("type", "keyword")
                    .field("index", false)
                    .endObject()
                    .startObject("id")
                    .field("type", "keyword")
                    .endObject()
                    .startObject("page")
                    .field("type", "nested")
                    .startObject("properties")
                    .startObject("content")
                    .field("type", "text")
                    .field("analyzer", "ik_max_word_lowercase")
                    .field("search_analyzer", "ik_smart_lowercase")
                    .endObject()
                    .startObject("imgPositions")
                    .field("type", "nested")
                    .startObject("properties")
                    .startObject("height")
                    .field("type", "float")
                    .endObject()
                    .startObject("width")
                    .field("type", "float")
                    .endObject()
                    .startObject("x")
                    .field("type", "float")
                    .endObject()
                    .startObject("y")
                    .field("type", "float")
                    .endObject()
                    .endObject()
                    .endObject()
                    .startObject("pageImagesContents")
                    .field("type", "text")
                    .field("analyzer", "ik_max_word_lowercase")
                    .field("search_analyzer", "ik_smart_lowercase")
                    .endObject()
                    .startObject("pageNumber")
                    .field("type", "integer")
                    .endObject()
                    .endObject()
                    .endObject()
                    .startObject("title")
                    .field("type", "text")
                    .field("analyzer", "ik_max_word_lowercase")
                    .field("search_analyzer", "ik_smart_lowercase")
                    .endObject()
                    .startObject("titleSuggest")
                    .field("type", "completion")
                    .field("analyzer", "simple")
                    .field("preserve_separators", true)
                    .field("preserve_position_increments", true)
                    .field("max_input_length", 50)
                    .endObject()
                    .startObject("type")
                    .field("type", "keyword")
                    .field("index", false)
                    .endObject()
                    .startObject("updateTime")
                    .field("type", "date")
                    .field("index", false)
                    .field("format", "yyyy-MM-dd HH:mm:ss")
                    .endObject()
                    .startObject("author")
                    .field("type", "keyword")
                    .endObject()
                    .endObject()
                    .endObject();
            request.mapping(mappingBuilder);
            // 创建索引
            client.indices().create(request, RequestOptions.DEFAULT);
        }

        BulkRequest bulkRequest = new BulkRequest();
        for (File file : files) {
            try {
                PDDocument document = documentPool.borrowObject().load(file);
                int pageCount = document.getNumberOfPages();
                for (int i = 0; i < pageCount; i++) {
                    List<String> imageContents = new ArrayList<>();
                    List<ImagePosition> imagePositions = new ArrayList<>();
                    String pageContent = StringUtils.deleteWhitespace(extractPageContent(document,file, i, imagePositions, imageContents));
                    String filePath = file.getPath();
                    String fileName = file.getName();
                    String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
                    String uuidTitle = fileName.substring(0, fileName.lastIndexOf("."));
                    String title = StringUtils.substringAfterLast(uuidTitle, "--");
                    String author = StringUtils.substringAfterLast(title,"_");
                    Completion titleSuggest = new Completion(new String[]{title});
                    Date date = new Date();
                    SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String fileId = StringUtils.substringBeforeLast(uuidTitle, "--");
                    String compositeKey = fileId + "-" + (i + 1);
                    article.setId(compositeKey);
                    article.setUpdateTime(simpleDate.format(date));
                    article.setTitle(title);
                    article.setTitleSuggest(titleSuggest);
                    article.setType(suffix);
                    article.setFileUrl(filePath);
                    article.setAuthor(author);
                    article.setPage(new Page(pageContent, i + 1, imagePositions, imageContents)); //images

                    // 将文章对象转换为JSON字符串，并创建IndexRequest对象
                    IndexRequest request = new IndexRequest("article")
                            .id(compositeKey)
                            .source(JSON.toJSONString(article), XContentType.JSON);

                    // 将IndexRequest对象添加到BulkRequest中
                    bulkRequest.add(request);
                }
            } catch (Exception e) {
                // PDFBox在处理某些PDF文件时可能会抛出异常，此时忽略该文件并继续处理下一个文件
                System.out.println("Error processing file " + file.getName() + ": " + e.getMessage());
            }
        }
//        documentPool.close();
        // 执行BulkRequest
        BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
        // 检查是否有上传失败的文档
        if (bulkResponse.hasFailures()) {
            System.out.println("Some documents failed to index:");
            for (BulkItemResponse bulkItemResponse : bulkResponse) {
                if (bulkItemResponse.isFailed()) {
                    System.out.println(bulkItemResponse.getFailureMessage());
                }
            }
        } else {
            System.out.println("All documents have been indexed successfully!");
        }
    }

    // PDFBox库提取PDF文件中指定页其中的文字、图片，以及记录图片位置信息
    private String extractPageContent(PDDocument document, File file, int pageNumber,  List<ImagePosition> imagePositions, List<String> imageContents) throws IOException {
        File imageFile = new File("src/main/resources/static/img/" +  "6.png");
        ExecutorService executorService  = Executors.newFixedThreadPool(2);
        PdfDocument pdfDocument  = new PdfDocument();
        pdfDocument.loadFromFile(file.getAbsolutePath());
        PdfPageBase page = pdfDocument.getPages().get(pageNumber);
        for (PdfImageInfo PdfImageInfo : page.getImagesInfo()){
            BufferedImage image = PdfImageInfo.getImage();
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            if (imageWidth > 1000 || imageHeight > 1000){//像素大小够大才有可能是有效图片
                //将图片写入磁盘
                ImageIO.write(image, "png", imageFile);
//                File i = new File("src/main/resources/static/temp/" +  UUID.randomUUID().toString() + ".png");
//                ImageIO.write(image,"png", i );
                //获取指定图片的边界属性
                Rectangle2D rect = PdfImageInfo.getBounds();
                //获取左上角坐标
                float x = (float)rect.getX();
                float y = (float)rect.getY();
                float width = (float)rect.getWidth();
                float height = (float)rect.getHeight();
                ImagePosition imagePosition = new ImagePosition();
                imagePosition.setX(x);
                imagePosition.setY(y);
                imagePosition.setWidth(width);
                imagePosition.setHeight(height);
                imagePositions.add(imagePosition);
                //OCR文字识别
                String[] command = {"D:/Python/Python310/python.exe", "src/main/resources/python/untitled3.py"};
                try {
                    Object lock = new Object();
                    executorService.execute(() -> {
                        try {
                            ProcessBuilder pb = new ProcessBuilder(command);
                            Process p = pb.start();
                            InputStreamReader inputStreamReader = new InputStreamReader(p.getInputStream(), "GBK");
                            BufferedReader bfr = new BufferedReader(inputStreamReader);
                            String line = bfr.readLine();
                            if (line != null) {//!StringUtils.isBlank(line)
                                System.out.println(line);
                                imageContents.add(line);
                            }
                            inputStreamReader.close();
                            boolean finished = p.waitFor(600000, TimeUnit.MILLISECONDS);
                            if (!finished) {
                                p.destroy();
                                System.err.println("Process timed out");
                            }
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            synchronized (lock) {
                                lock.notify();
                            }
                        }
                    });
                    synchronized (lock) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
            }

        }
        imageFile.delete();

        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(pageNumber + 1);
        stripper.setEndPage(pageNumber + 1);
        return stripper.getText(document);
    }
}
