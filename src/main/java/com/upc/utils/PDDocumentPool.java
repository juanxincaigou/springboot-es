package com.upc.utils;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

@Component
public class PDDocumentPool {

    private static final int MAX_TOTAL = 80;
    private static final int MAX_IDLE = 10;
    private static final int MIN_IDLE = 2;
    private static final long MAX_WAIT_MILLIS = 600000; // 10分钟

    private final GenericObjectPool<PDDocument> pool;

    public PDDocumentPool() {
        GenericObjectPoolConfig<PDDocument> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(MAX_TOTAL);  //池中允许的最大对象数
        config.setMaxIdle(MAX_IDLE);    //池中允许的最大空闲对象数
        config.setMinIdle(MIN_IDLE);    //池中保持的最小空闲对象数,当池中的对象数量少于 minIdle 时，池中将创建新的对象
        //池中对象的最大等待时间。当池中的对象都被借出并且池中没有空闲对象时，
        // 后续请求对象的线程将阻塞等待，直到等待时间超过 maxWaitMillis 或者有一个对象被归还到池中
        config.setMaxWaitMillis(MAX_WAIT_MILLIS);


        pool = new GenericObjectPool<>(new PDDocumentFactory(), config);
    }

    public synchronized PDDocument borrowObject() throws Exception {
        return pool.borrowObject();
    }

    public synchronized void returnObject(PDDocument pdDocument) {
        pool.returnObject(pdDocument);
    }

    public void close() {
        pool.close();
    }
}


