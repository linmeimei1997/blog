package com.blog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * 重试配置
 */
@Configuration
@EnableRetry
public class RetryConfig {
}
