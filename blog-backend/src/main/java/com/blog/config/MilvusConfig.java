package com.blog.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.R;
import io.milvus.param.collection.HasCollectionParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Milvus 向量数据库配置
 */
@Slf4j
@Configuration
public class MilvusConfig {

    @Value("${milvus.host:localhost}")
    private String host;

    @Value("${milvus.port:19530}")
    private int port;

    @Value("${milvus.database:blog}")
    private String database;

    @Bean
    @ConditionalOnProperty(name = "milvus.enabled", havingValue = "true")
    public MilvusServiceClient milvusClient() {
        try {
            ConnectParam connectParam = ConnectParam.newBuilder()
                    .withHost(host)
                    .withPort(port)
                    .withDatabaseName(database)
                    .withConnectTimeout(3, TimeUnit.SECONDS)
                    .build();
            
            MilvusServiceClient client = new MilvusServiceClient(connectParam);
            
            // 测试连接
            R<Boolean> testResult = client.hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName("test_connection")
                            .build()
            );
            
            log.info("Milvus 向量数据库连接成功: {}:{}", host, port);
            return client;
        } catch (Exception e) {
            log.warn("Milvus 连接失败，将使用内存向量存储: {}", e.getMessage());
            return null;
        }
    }
}
