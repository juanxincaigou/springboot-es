package com.upc;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.spire.pdf.FileFormat;
import com.spire.pdf.PdfDocument;
import com.spire.pdf.PdfPageBase;
import com.spire.pdf.exporting.PdfImageInfo;
import com.spire.pdf.general.find.PdfTextFind;
import com.spire.pdf.general.find.TextFindParameter;
import com.spire.pdf.graphics.PdfPen;
import com.spire.pdf.graphics.PdfRGBColor;
import com.upc.dao.ArticleRepository;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.io.FileUtils.deleteDirectory;

@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
public class EsTest {

    @Autowired
    private ArticleRepository articleRepository;

    @Resource
    private RestHighLevelClient client;

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Test
    public void Test1() {
        File folder = new File("D:/服务外包/企业数据集/企业数据集提供/新建文件夹");
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                String fileName = UUID.randomUUID() + "--" + file.getName();
                String filePath = file.getPath();
                String uuidTitle = fileName.substring(0, fileName.lastIndexOf("."));
                String uuid = StringUtils.substringBeforeLast(uuidTitle, "--");
                String title = StringUtils.substringAfterLast(uuidTitle, "--");
                String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
                System.out.println(fileName);
                System.out.println(filePath);
                System.out.println(uuid);
                System.out.println(title);
                System.out.println(suffix);
            }
        }


    }

    @Test
    public void Test2() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date date = new Date();
        System.out.println("系统当前时间" + df.format(date));
    }

    @Test
    public void Test3() {
//        //        分页查询全部
//        Pageable pageable = PageRequest.of(0, 2);
//        Page<Article> page = articleRepository.findAll(pageable);
//        for (Article article : page.getContent()) {
//            System.out.println(article);
//        }
//        List<Article> articles = articleRepository.findByTitleLike("网络");
//        for (Article article : articles) {
//            System.out.println(article);
//        }
    }

    @Test
    public void Test4() throws IOException {
        String indexName = "article";
        String searchValue = "甲状腺";
        String nestedField = "page";
        String fieldName = "title";

//        FunctionScoreQueryBuilder.FilterFunctionBuilder[] functions = {
//                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
//                        QueryBuilders.nestedQuery(nestedField,
//                                QueryBuilders.boolQuery()
//                                        .should(QueryBuilders.matchQuery(nestedField + ".content", searchValue).analyzer("ik_smart"))
//                                , ScoreMode.Avg),   //将嵌套文档得分的平均值作为父文档得分
//                        ScoreFunctionBuilders.weightFactorFunction(2.0f)
//                )
//        };
//
//        FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(functions)
//                .scoreMode(FunctionScoreQuery.ScoreMode.SUM)
//                .boostMode(CombineFunction.SUM);

        QueryBuilder queryBuilder = QueryBuilders.nestedQuery(nestedField,
                QueryBuilders.boolQuery()
                        .should(QueryBuilders.matchQuery(nestedField + ".content", searchValue))
//                        .should()//写图片的检索
                , ScoreMode.Avg);   //将嵌套文档得分的平均值作为父文档得分

        HighlightBuilder highlightBuilder = new HighlightBuilder()      //生成高亮查询器
                .field(fieldName)
                .field(nestedField + ".content");                       //添加字段

        highlightBuilder.requireFieldMatch(false);                      //如果要多个字段高亮,这项要为false
        highlightBuilder.preTags("<span style=\"color:yellow\">");      //高亮设置
        highlightBuilder.postTags("</span>");

        //要高亮如文字内容等有很多字的字段,必须配置,不然会导致高亮不全,文章内容缺失等
        highlightBuilder.fragmentSize(800000);                          //最大高亮分片数
        highlightBuilder.numOfFragments(0);                             //从第一个分片获取高亮片段

        SortBuilder sortBuilder = SortBuilders.fieldSort("_score").order(SortOrder.DESC);

        int pageNum = 1; // 指定当前页码
        int pageSize = 4; // 指定每页显示数量
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(queryBuilder)
                .highlighter(highlightBuilder)
//                .explain(true)      //设置是否返回查询解释信息，即查询的详细说明和计算过程
//                .trackScores(true)  //设置是否返回每个文档的得分信息
                .from((pageNum - 1) * pageSize)
                .size(pageSize)
                .sort(sortBuilder)
                //指定要获取的字段,第一个参数为包含的字段，第二个参数为不包含的字段
                .fetchSource(new String[]{"title", nestedField + "content", "_score"}, null);

        SearchRequest searchRequest = new SearchRequest()
                .indices(indexName)
                .source(searchSourceBuilder);


        // 发送搜索请求并获取响应结果
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        SearchHits hits = searchResponse.getHits();
        //最后需要返回的map
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("totalHits", hits.getTotalHits().value);

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (SearchHit hit : hits.getHits()) { //每一页
            //获取其源数据Map对象
            Map<String, Object> hitMap = hit.getSourceAsMap();
            // 处理高亮显示结果
            //获取命中文档中所有高亮字段和对应的高亮结果Map对象
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (highlightFields != null && !highlightFields.isEmpty()) {
                //从高亮结果中获取特定字段的高亮结果
                HighlightField highlightTitleField = highlightFields.get(fieldName);
                HighlightField highlightNestedField = highlightFields.get(nestedField + ".content");
                //如果存在高亮结果则将其替换原文本
                if (highlightTitleField != null) {
                    hitMap.put(fieldName, highlightTitleField.fragments()[0].string());
                }
                if (highlightNestedField != null) {
                    hitMap.put(nestedField + ".content", highlightNestedField.fragments()[0].string());
                }
                //获取到高亮片段，然后统计高亮片段中命中关键词的次数
                Text[] fragments = highlightNestedField.getFragments();
                if (fragments != null && fragments.length > 0) {
                    int hitCount = 0;
                    for (Text fragment : fragments) {
                        hitCount += StringUtils.countMatches(fragment.string(), searchValue);
                    }
                    hitMap.put("hitCount", hitCount);
                }
            }
            //这里在插入list时可以先比较hitCount，然后按高低插入
            resultList.add(hitMap);
        }
        resultMap.put("resultList", resultList);
//        System.out.println(resultMap);
    }


    @Test
    public void Test5() {
        String indexName = "article";
        String keyWords = "肿瘤";
        String nestedField = "page";
        String fieldName = "title";
        QueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .should(QueryBuilders.nestedQuery(nestedField,
                        QueryBuilders.matchQuery(nestedField + ".content", keyWords),
                        ScoreMode.Avg))
                .should(QueryBuilders.nestedQuery(nestedField,
                        QueryBuilders.matchQuery(nestedField + ".pageImagesContents", keyWords),
                        ScoreMode.Avg));//将嵌套文档得分的平均值作为父文档得分

        HighlightBuilder highlightBuilder = new HighlightBuilder()
//                .field(fieldName)
                .field(nestedField + ".pageImagesContents")
                .field(nestedField + ".content");

        highlightBuilder.requireFieldMatch(false);                      //如果要多个字段高亮,这项要为false
        highlightBuilder.preTags("<span style=\"color:yellow\">");      //高亮设置
        highlightBuilder.postTags("</span>");

        //要高亮如文字内容等有很多字的字段,必须配置,不然会导致高亮不全,文章内容缺失等
        highlightBuilder.fragmentSize(800000);                          //最大高亮分片数
        highlightBuilder.numOfFragments(0);                             //从第一个分片获取高亮片段


        int pageNum = 1; // 指定当前页码
        int pageSize = 10000; // 指定每页显示数量
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(queryBuilder)
                .highlighter(highlightBuilder)
                .from((pageNum - 1) * pageSize)
                .size(pageSize)
                .sort(SortBuilders.scoreSort().order(SortOrder.DESC))
                //        SortBuilders.fieldSort("_score").order(SortOrder.DESC)        根据指定字段排序
                //指定要获取的字段,第一个参数为包含的字段，第二个参数为不包含的字段
                .fetchSource(new String[]{fieldName, nestedField + ".pageNumber"}, null);


        SearchRequest searchRequest = new SearchRequest()
                .indices(indexName)
                .source(searchSourceBuilder);


        // 发送搜索请求并获取响应结果
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

//         处理查询结果
        //获取命中的文档
        SearchHits hits = searchResponse.getHits();
        //最后需要返回的map
        Map<String, Object> resultMap = new HashMap<>();
        HashSet<Map<String, Object>> articleIdAndTitleSet = new HashSet<>();

        List<Map<String, Object>> articleList = new ArrayList<>();  //存每个pdf

        for (SearchHit hit : hits.getHits()) {
            Map<String, Object> idAndTitle = new HashMap<>();
            idAndTitle.put("articleId", StringUtils.substringBeforeLast(hit.getId(), "-"));
            //普通title
            Map<String, Object> hitPage = hit.getSourceAsMap();
            idAndTitle.put("articleTitle", hitPage.get("title"));
            //如果pdf题目中命中关键字，替换为高亮的
            if (hit.getHighlightFields() != null && !hit.getHighlightFields().isEmpty()) {
                if (hit.getHighlightFields().get("title") != null) {
                    idAndTitle.put("articleTitle", hit.getHighlightFields().get("title").fragments()[0].string());
                }
            }
            articleIdAndTitleSet.add(idAndTitle);
        }

        //命中的pdf数
        resultMap.put("totalHits", articleIdAndTitleSet.size());

        for (Map<String, Object> idAndTitle : articleIdAndTitleSet) {
            Map<String, Object> article = new HashMap<>();
            article.put("articleId", idAndTitle.get("articleId"));
            article.put("articleTitle", idAndTitle.get("articleTitle"));
            article.put("pageList", new ArrayList<>());
            articleList.add(article);
            resultMap.put("articleList", articleList);
        }

        //存每个pdf中的页
        List<Map<String, Object>> pageList;
        for (SearchHit hit : hits.getHits()) {
            String articleId = StringUtils.substringBeforeLast(hit.getId(), "-");
            for (Map<String, Object> article : articleList) {
                if (articleId.equals(article.get("articleId").toString())) {
                    //获取单页源数据Map对象
                    Map<String, Object> hitPage = hit.getSourceAsMap();
                    //添加每页得分
                    hitPage.put("pageScore", hit.getScore());
                    //页数，本来是个hashmap{pageNumber=9}这种，利用键名一致会自动替换值，换成数字
                    HashMap<String, Object> page = (HashMap<String, Object>) hitPage.get("page");
                    hitPage.put("page", page.get("pageNumber"));
                    // 处理高亮显示结果
                    //获取命中文档中所有高亮字段和对应的高亮结果Map对象
                    Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                    if (highlightFields != null && !highlightFields.isEmpty()) {
                        //从高亮结果中获取特定字段的高亮结果
                        HighlightField highlightNestedField = highlightFields.get(nestedField + ".content");
                        //如果存在高亮结果则将其替换原文本
                        if (highlightNestedField != null) {
                            hitPage.put("content", highlightNestedField.fragments()[0].string());
                        }
                        HighlightField highlightImageContentsField = highlightFields.get(nestedField + ".pageImagesContents");
                        if (highlightImageContentsField != null) {
                            hitPage.put("pageImageContents", highlightImageContentsField.fragments()[0].string());
                        }
                        //获取到高亮片段，然后统计高亮片段中命中关键词的次数
                        Text[] fragments = highlightNestedField.getFragments();
                        if (fragments != null && fragments.length > 0) {
                            int hitCount = 0;
                            for (Text fragment : fragments) {
                                hitCount += StringUtils.countMatches(fragment.string(), keyWords);
                            }
                            hitPage.put("hitCount", hitCount);
                        }
                    }
                    pageList = (List<Map<String, Object>>) article.get("pageList");
                    pageList.add(hitPage);
                }

            }
        }
        for (Map<String, Object> article : articleList) {
            int articleHitCount = 0;
            List<Map<String, Object>> list = (List<Map<String, Object>>) article.get("pageList");
            for (Map<String, Object> page : list) {
                articleHitCount += Integer.parseInt(page.get("hitCount").toString());
            }
            article.put("articleHitCount", articleHitCount);
        }
        System.out.println(resultMap);
    }


    @Test
    public void Test6() {
        System.out.println(StringUtils.substringBeforeLast("f0230b90-82b1-403f-a690-ad29ded5aa7d-1", "-"));
    }

    @Test
    public void Test7() {
        ITesseract instance = new Tesseract();
        instance.setDatapath("tessdata"); //tessdata目录和src目录平级
        instance.setLanguage("chi_sim");//选择字库文件（只需要文件名，不需要后缀名）
        try {
            File imageFile = new File("src/main/resources/static/img.png");
            String result = instance.doOCR(imageFile);      //开始识别
            System.out.println(result);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @Test
    public void Test8() throws IOException {
    }

    @Test
    public void Test9() {
        List<Map<String, Object>> list = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", 1);
            map.put("name", "test" + i);
            map.put("age", random.nextInt(100));
            list.add(map);
        }
        System.out.println("排序前：---");
        list.forEach(System.out::println);
        list.sort((o1, o2) -> {
            Integer age = Integer.valueOf(o1.getOrDefault("age", Integer.MIN_VALUE).toString());
            Integer age1 = Integer.valueOf(o2.getOrDefault("age", Integer.MIN_VALUE).toString());
//            return age.compareTo(age1); // 升序
            return age1.compareTo(age); // 降序
        });
        System.out.println("排序后：---");
        list.forEach(System.out::println);

    }

    @Test
    public void Test10() throws IOException {
        PDDocument document = PDDocument.load(new File("src/main/resources/static/2016IJC-导管组织接触对于模型的影响PentarRay FAM.pdf"));
        PDFRenderer renderer = new PDFRenderer(document);
        BufferedImage image = renderer.renderImageWithDPI(1, 300);
        ImageIO.write(image, "png", new File("src/main/resources/static/page1.png"));
        document.close();
    }

    private static final String UPLOAD_DIR = "/static/uploads";

    @Test
    public void Test11() {
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdir();
        }
        MultipartFile files = null;
        String fileName = UUID.randomUUID() + "-" + files.getOriginalFilename();
        try {
            File dest = new File(uploadDir.getCanonicalFile() + "/" + fileName);
            files.transferTo(dest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void Test12() {
        try {
//            File file = new File("src/main/resources/static/img/");
            List<String> command = new ArrayList<>();
            command.add("D:/Python/Python310/python.exe");
            command.add("src/main/resources/python/untitled3.py");
//            command.add(file.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(command);
            Process p = pb.start();

            BufferedReader bfr = new BufferedReader(new InputStreamReader(p.getInputStream(), "GBK"));

            System.out.println(".........start   process.........");
            String line = "";
            if ((line = bfr.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("........end   process.......");

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @Test
    public void Test13() throws IOException {
        File ocrResult = new File("src/main/resources/static/img/ocr_result");
        String[] command = {"D:/Python/Python310/python.exe", "src/main/resources/python/untitled3.py"};
        ExecutorService executorService = Executors.newFixedThreadPool(40);
        if (ocrResult.exists()) {
            deleteDirectory(ocrResult);
        }

        executorService.execute(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.inheritIO();
                Process p = pb.start();
                boolean finished = p.waitFor(300000, TimeUnit.MILLISECONDS);
                if (!finished) {
                    p.destroy();
                    System.err.println("Process timed out");
                }

                BufferedReader bfr = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                if ((line = bfr.readLine()) != null) {
                    String result = StringUtils.deleteWhitespace(line);
                    System.out.println(result);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    public void Test14() throws IOException {
        File ocrResult = new File("src/main/resources/static/img/ocr_result");
        String[] command = {"D:/Python/Python310/python.exe", "src/main/resources/python/untitled3.py"};
        ExecutorService executorService = Executors.newFixedThreadPool(40);
        if (ocrResult.exists()) {
            deleteDirectory(ocrResult);
        }

        Object lock = new Object();
        executorService.execute(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.inheritIO();
                Process p = pb.start();
                boolean finished = p.waitFor(300000, TimeUnit.MILLISECONDS);
                if (!finished) {
                    p.destroy();
                    System.err.println("Process timed out");
                }

                BufferedReader bfr = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                if ((line = bfr.readLine()) != null) {
                    String result = StringUtils.deleteWhitespace(line);
                    System.out.println(result);
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

    }

    @Test
    public void Test15() {
        String encoding = System.getProperty("file.encoding");
        System.out.println("Current encoding: " + encoding);
    }

    @Test
    public void Test16() throws IOException {

        //保存pdf某一页到新的文件
//        PDDocument document = PDDocument.load(new File("src/main/resources/static/uploads/d7c3efb8-094e-4ca9-8ab5-b6322e3468a3--基于新冠病毒核衣壳蛋白N端...构的抗病毒药物设计（英文）_栾晓东.pdf"));
//        PDPage page = document.getPage(3);
//        PDDocument newDocument = new PDDocument();
//        newDocument.addPage(page);
//        newDocument.save(new File("new.pdf"));

    }

    @Test
    public  void  Test17() throws IOException{
        PdfDocument doc = new PdfDocument();
        doc.loadFromFile("src/main/resources/static/uploads/95fe99b5-5b46-421f-b072-071c6feb5249--靶向新型冠状病毒SARS-...蛋白酶的抗病毒药物研究进展_王春霞.pdf");
        PdfPageBase page = doc.getPages().get(3);
        PdfImageInfo[] imageInfo = page.getImagesInfo();
        int index = 0;
        //遍历图片信息
        for (int i = 0; i < imageInfo.length; i++) {

            //获取指定图片的边界属性
            Rectangle2D rect = imageInfo[i].getBounds();

            //获取左上角坐标
            System.out.println(String.format("第%d张图片的左上角坐标为：（%f, %f）,宽高为 （%f, %f）", i+1, rect.getX(), rect.getY(),rect.getWidth(),rect.getHeight()));

            Rectangle2D.Float rect1 = new Rectangle2D.Float((float) rect.getX(),(float)  rect.getY(), (float) rect.getWidth(),(float) rect.getHeight() );
            //绘制黄色矩形
            PdfPen pen = new PdfPen(new PdfRGBColor(Color.yellow), 2);
            page.getCanvas().drawRectangle(pen, rect1);

            PdfDocument newDoc = new PdfDocument();
            newDoc.insertPage(doc,3);
//            newDoc.saveToFile("DrawShapes.pdf", FileFormat.PDF);

            String id = StringUtils.substringBefore(UUID.randomUUID().toString(), "-");
            newDoc.saveToFile("src/main/resources/static/temp/highLightShape" + id + ".pdf", FileFormat.PDF);

        }

//
//        for (BufferedImage image : page.extractImages()) {
//
//            //指定文件路径和文件名
//            File output = new File("src/main/resources/static/img/" + imgName + ".png");
//
//            //将图片保存为PNG文件
//            ImageIO.write(image, "PNG", output);
//        }

    }


    @Test
    public  void Test18() throws IOException{
        //创建 PdfDocument 类的对象
        PdfDocument doc = new PdfDocument();

        //载入PDF文档
        doc.loadFromFile("src/main/resources/static/uploads/95fe99b5-5b46-421f-b072-071c6feb5249--靶向新型冠状病毒SARS-...蛋白酶的抗病毒药物研究进展_王春霞.pdf");
        //声明一个int变量
        int index = 0;

        //循环遍历所有页面
        for (PdfPageBase page : (Iterable<PdfPageBase>) doc.getPages()) {
            BufferedImage[] bufferedImages = page.extractImages();
            if(bufferedImages != null){
                //从页面中提取图片
                for (BufferedImage image : page.extractImages()) {
                    int width = image.getWidth();
                    int height = image.getHeight();

                    if (width > 1000 || height > 1000){
                        //指定文件路径和文件名
                        File output = new File("src/main/resources/static/temp/" + String.format("图片-%d.png", index++));

                        //将图片保存为PNG文件
                        ImageIO.write(image, "PNG", output);
                    }
                }
            }
        }
    }

    @Test
    public void Test19()throws IOException{
        //创建 PdfDocument 类的对象
        PdfDocument doc = new PdfDocument();

        //载入PDF文档
        doc.loadFromFile("src/main/resources/static/uploads/95fe99b5-5b46-421f-b072-071c6feb5249--靶向新型冠状病毒SARS-...蛋白酶的抗病毒药物研究进展_王春霞.pdf");
        //声明一个int变量
        int index = 0;

        //循环遍历所有页面
        for (PdfPageBase page : (Iterable<PdfPageBase>) doc.getPages()) {
            BufferedImage[] bufferedImages = page.extractImages();
            if(bufferedImages != null){
                //从页面中提取图片
                for (BufferedImage image : page.extractImages()) {
                    //指定文件路径和文件名
                    File output = new File("src/main/resources/static/temp/" + String.format("图片-%d.png", index++));
                    //图片相关的操作
                    BufferedImage convertedImage = new BufferedImage(image.getWidth(), image.getHeight(), 1);
                    convertedImage.createGraphics().drawRenderedImage(image, null);

                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    ImageIO.write(convertedImage, "png", byteArrayOutputStream);
                    byte[] imageBytes = byteArrayOutputStream.toByteArray();

                    //将图片写入磁盘
                    FileOutputStream fos = new FileOutputStream(output);
                    fos.write(imageBytes);

    //                File ocrResult = new File("src/main/resources/static/img/ocr_result");
    //                if (ocrResult.exists()){
    //                    deleteDirectory(ocrResult);
    //                }
                }
            }
        }
    }

    @Test
    public  void Test20(){
        File pdfFile = new File("src/main/resources/static/uploads/ffad1e30-6b60-41dd-8fc5-fb8bfdb6a974--基于新冠病毒核衣壳蛋白N端...构的抗病毒药物设计（英文）_栾晓东.pdf");
        String outputFile = "output.pdf";
        String keyText = "RNA";
        PdfDocument pdf=new PdfDocument();
        pdf.loadFromFile(pdfFile.getAbsolutePath());
        PdfTextFind[] result = null;
        for (Object pageObj : pdf.getPages()) {
            PdfPageBase page = (PdfPageBase) pageObj;
            // Find text
            result = page.findText(keyText, EnumSet.of(TextFindParameter.None)).getFinds();
            if (result != null) {
                for (PdfTextFind find : result) {
                    find.applyHighLight(Color.yellow);
                }
            }
        }
        pdf.saveToFile(outputFile, FileFormat.PDF);
        pdf.close();

    }

    @Test
    public void test1111(){
        String keyword = "肾上腺激素";
        String apiUrl = "https://api.ownthink.com/kg/knowledge";
        HttpURLConnection connection = null;
        BufferedReader   reader = null;
        try {
            URL url = new URL(apiUrl + "?entity=" + keyword);
            System.out.println(url);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();// 建立TCP连接
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                System.out.println(result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            connection.disconnect();
        }
    }

    @Test
    public void test1112(){
        HttpURLConnection connection = null;
        InputStream is = null;
        OutputStream os = null;
        BufferedReader br = null;
        String result = null;
        JSONObject json = new JSONObject();
        json.put("entity","肾上腺激素");
        String param = json.toJSONString();
        try {
            URL url = new URL("https://api.ownthink.com/kg/knowledge");
            // 通过远程url连接对象打开连接
            connection = (HttpURLConnection) url.openConnection();
            // 设置连接请求方式
            connection.setRequestMethod("POST");
            // 设置连接主机服务器超时时间：15000毫秒
            connection.setConnectTimeout(15000);
            // 设置读取主机服务器返回数据超时时间：60000毫秒
            connection.setReadTimeout(60000);

            // 默认值为：false，当向远程服务器传送数据/写数据时，需要设置为true
            connection.setDoOutput(true);
            // 默认值为：true，当前向远程服务读取数据时，设置为true，该参数可有可无
            connection.setDoInput(true);
            // 设置传入参数的格式:请求参数应该是 name1=value1&name2=value2 的形式。
            connection.setRequestProperty("Content-Type", "application/json");
            // 设置鉴权信息：Authorization: Bearer da3efcbf-0845-4fe3-8aba-ee040be542c0
            //connection.setRequestProperty("Authorization", "Bearer da3efcbf-0845-4fe3-8aba-ee040be542c0");
            // 通过连接对象获取一个输出流
            os = connection.getOutputStream();
            // 通过输出流对象将参数写出去/传输出去,它是通过字节数组写出的
            os.write(param.getBytes());
            // 通过连接对象获取一个输入流，向远程读取
            if (connection.getResponseCode() == 200) {

                is = connection.getInputStream();
                // 对输入流对象进行包装:charset根据工作项目组的要求来设置
                br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

                StringBuffer sbf = new StringBuffer();
                String temp = null;
                // 循环遍历一行一行读取数据
                while ((temp = br.readLine()) != null) {
                    sbf.append(temp);
                    sbf.append("\r\n");
                }
                result = sbf.toString();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
            if (null != br) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != os) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            connection.disconnect();
        }
        Map map =(Map)JSON.parse(result);
        Map data = (Map)map.get("data");
        JSONArray jsonArray = (JSONArray)data.get("avp");
        for (Object o : jsonArray) {
            JSONArray item = ( JSONArray)o;
            System.out.println(item.get(0));
        }
    }
}





