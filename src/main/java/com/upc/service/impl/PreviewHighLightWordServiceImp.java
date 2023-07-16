package com.upc.service.impl;

import com.spire.pdf.PdfDocument;
import com.spire.pdf.PdfPageBase;
import com.spire.pdf.general.find.PdfTextFind;
import com.spire.pdf.general.find.PdfTextFindCollection;
import com.spire.pdf.general.find.TextFindParameter;
import com.upc.service.PreviewHighLightWordService;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.EnumSet;


@Service
public class PreviewHighLightWordServiceImp implements PreviewHighLightWordService {
    @Override
    public byte[] previewHighLightWord(String keyWord, File file, String fileName, int pageNumber) {
        PdfDocument pdfDocument = new PdfDocument();
        pdfDocument.loadFromFile(file.getAbsolutePath());
        PdfPageBase page = pdfDocument.getPages().get(pageNumber - 1);

        PdfTextFind[] result = null;
        PdfTextFindCollection finds = page.findText(keyWord, EnumSet.of(TextFindParameter.None));
        if (finds != null) {
            result = finds.getFinds();
        }
        if (result != null) {
            for (PdfTextFind find : result) {
                find.applyHighLight(Color.yellow);
            }
        }
        PdfDocument newDoc = new PdfDocument();
        newDoc.insertPage(pdfDocument,pageNumber - 1);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        newDoc.saveToStream(outputStream);
        return outputStream.toByteArray();
    }
}
