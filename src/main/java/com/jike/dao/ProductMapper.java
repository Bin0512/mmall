package com.jike.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.jike.pojo.Product;

public interface ProductMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Product record);

    int insertSelective(Product record);

    Product selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Product record);

    int updateByPrimaryKey(Product record);
    
    List<Product> selectList();
    
    List<Product> selectByNameAndProductId(@Param("productName") String productName,@Param("productId") Integer productId);
}