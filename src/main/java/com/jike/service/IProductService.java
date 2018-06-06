package com.jike.service;

import com.github.pagehelper.PageInfo;
import com.jike.common.ServerResponse;
import com.jike.pojo.Product;
import com.jike.vo.ProductDetailVo;

public interface IProductService {

	/**
	 * 新增或者更新产品信息
	 * @param product
	 * @return
	 */
	ServerResponse saveOrUpdateProduct(Product product);

	/**
	 * 根据产品ID更改产品的在售状态
	 * @param productId
	 * @param status
	 * @return
	 */
	ServerResponse<String> setSaleStatus(Integer productId, Integer status);

	/**
	 * 根据产品ID展示商品详情
	 * @param productId
	 * @return
	 */
	ServerResponse<ProductDetailVo> manageProductDetail(Integer productId);

	/**
	 * 获取产品列表，并进行分页显示
	 * @param pageNum
	 * @param pageSize
	 * @return
	 */
	ServerResponse<PageInfo> getProductList(int pageNum, int pageSize);

	/**
	 * 根据产品名字或者产品编号进行动态查询
	 * @param productName
	 * @param productId
	 * @param pageNum
	 * @param pageSize
	 * @return
	 */
	ServerResponse<PageInfo> searchProduct(String productName, Integer productId, int pageNum, int pageSize);

	/**
	 * 根据产品Id获取产品详情
	 * @param productId
	 * @return
	 */
	ServerResponse<ProductDetailVo> getProductDetail(Integer productId);

	/**
	 * todo
	 * @param keyword
	 * @param categoryId
	 * @param pageNum
	 * @param pageSize
	 * @param orderBy
	 * @return
	 */
	ServerResponse<PageInfo> getProductByKeywordCategory(String keyword, Integer categoryId, int pageNum, int pageSize,
			String orderBy);

}
