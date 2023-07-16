package com.upc.utils;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;

public class PDDocumentFactory extends BasePooledObjectFactory<PDDocument> {
    @Override
    public PDDocument create() throws IOException {
        return new PDDocument();
    }

    @Override
    public void destroyObject(PooledObject<PDDocument> p) throws Exception {
        PDDocument doc = p.getObject();
        if (doc != null) {
            doc.close();
        }
    }

    @Override
    public boolean validateObject(PooledObject<PDDocument> p) {
        PDDocument doc = p.getObject();
        return doc != null && !isDocumentClosed(doc);
    }

    private boolean isDocumentClosed(PDDocument doc){
        doc.getNumberOfPages();
        return false;
    }

    @Override
    public PooledObject<PDDocument> wrap(PDDocument pdDocument) {
        return new DefaultPooledObject<>(pdDocument);
    }
}
