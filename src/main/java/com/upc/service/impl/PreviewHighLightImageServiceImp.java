package com.upc.service.impl;

import com.spire.pdf.PdfDocument;
import com.spire.pdf.PdfPageBase;
import com.spire.pdf.graphics.PdfPen;
import com.spire.pdf.graphics.PdfRGBColor;
import com.upc.pojo.ImagePosition;
import com.upc.service.PreviewHighLightImageService;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.File;

@Service
public class PreviewHighLightImageServiceImp implements PreviewHighLightImageService {

    @Override
    public byte[] previewHighLightImage(ImagePosition imagePosition, File file, String fileName, int pageNumber) {
        PdfDocument pdfDocument = new PdfDocument();
        pdfDocument.loadFromFile(file.getAbsolutePath());
        PdfPageBase page = pdfDocument.getPages().get(pageNumber - 1);
        float x = imagePosition.getX();
        float y = imagePosition.getY();
        float width = imagePosition.getWidth();
        float height = imagePosition.getHeight();
        Rectangle2D.Float rect = new Rectangle2D.Float(x,y,width,height);
        //绘制黄色矩形
        PdfPen pen = new PdfPen(new PdfRGBColor(Color.yellow), 2);
        page.getCanvas().drawRectangle(pen, rect);

        PdfDocument newDoc = new PdfDocument();
        newDoc.insertPage(pdfDocument,pageNumber - 1);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        newDoc.saveToStream(outputStream);
        return outputStream.toByteArray();
    }
}
