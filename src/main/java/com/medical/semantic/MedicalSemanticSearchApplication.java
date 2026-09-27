package com.medical.semantic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 医学语义搜索应用
 *
 * 基于向量数据库的医学文献智能检索系统
 */
@SpringBootApplication
public class MedicalSemanticSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicalSemanticSearchApplication.class, args);
    }
}
