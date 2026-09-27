package com.medical.semantic.repository;

import com.medical.semantic.domain.MedicalDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文档数据访问接口
 */
@Mapper
public interface DocumentMapper {

    /**
     * 插入文档
     */
    void insert(MedicalDocument document);

    /**
     * 根据 ID 查询
     */
    MedicalDocument findById(@Param("id") Long id);

    /**
     * 查询所有文档
     */
    List<MedicalDocument> findAll();

    /**
     * 根据类型查询
     */
    List<MedicalDocument> findByType(@Param("type") String type);

    /**
     * 更新文档
     */
    void update(MedicalDocument document);

    /**
     * 删除文档
     */
    void delete(@Param("id") Long id);

    /**
     * 关键词搜索（使用 MySQL FULLTEXT 索引）
     */
    List<MedicalDocument> keywordSearch(@Param("query") String query);

    /**
     * 统计指定类型的文档数量
     */
    long countByType(@Param("type") String type);

    /**
     * 统计总文档数
     */
    long countTotal();
}
