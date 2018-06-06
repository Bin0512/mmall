package com.jike.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.jike.pojo.Cart;
import com.jike.pojo.Product;

public interface CartMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Cart record);

    int insertSelective(Cart record);

    Cart selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Cart record);

    int updateByPrimaryKey(Cart record);

	Cart selectCartByUserIdProductId(@Param("userId")Integer userId, @Param("productId")Integer productId);
	
	List<Cart> selectCartByUserId(Integer userId);

	int selectCartProductCheckedStatusByUserId(Integer userId);

	int deleteByUserIdProductIds(Integer userId, List<String> productList);

	int checkedOrUncheckedProduct(Integer userId, Integer productId, Integer checked);

	int selectCartProductCount(Integer userId);

}