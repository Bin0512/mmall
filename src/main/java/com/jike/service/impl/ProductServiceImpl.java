package com.jike.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.annotation.Resource;
import javax.xml.crypto.dsig.keyinfo.RetrievalMethod;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.jike.common.Const;
import com.jike.common.ResponseCode;
import com.jike.common.ServerResponse;
import com.jike.dao.CategoryMapper;
import com.jike.dao.ProductMapper;
import com.jike.pojo.Category;
import com.jike.pojo.Product;
import com.jike.service.IProductService;
import com.jike.util.DateTimeUtil;
import com.jike.util.PropertiesUtil;
import com.jike.vo.ProductDetailVo;
import com.jike.vo.ProductListVo;

import ch.qos.logback.classic.joran.action.InsertFromJNDIAction;

@Service("iProductService")
public class ProductServiceImpl implements IProductService {

	@Resource
	private ProductMapper productMapper;
	
	@Resource
	private CategoryMapper categoryMapper;

	@Override
	public ServerResponse saveOrUpdateProduct(Product product) {
		// TODO Auto-generated method stub
		//1、对参数进行非空验证
		if (product != null) {
			//2、设置主图
			if (StringUtils.isNotEmpty(product.getSubImages())) {
				String[] imgList = product.getSubImages().split(".");
				if (imgList.length > 0) {
					//3、默认将第一章图片设为主图
					product.setMainImage(imgList[0]);
				}
			}
			//4、根据有没有产品ID，来执行更新或者新增的操作
			int resultCount = -1;
			if (product.getId() == null) {
				//执行新增操作
				resultCount = productMapper.insert(product);
				if (resultCount > 0) {
					return ServerResponse.createBySuccessMessage("新增产品成功");
				}
				return ServerResponse.createByErrorMessage("新增产品失败");
			}else {
				//执行更新操作
				resultCount = productMapper.updateByPrimaryKeySelective(product);
				if (resultCount > 0) {
					return ServerResponse.createBySuccessMessage("更新产品成功");
				}
				return ServerResponse.createByErrorMessage("更新产品失败");
			}
		}
		return ServerResponse.createByErrorMessage("新增或更新产品参数不正确");
	}

	@Override
	public ServerResponse<String> setSaleStatus(Integer productId, Integer status) {
		// 1、判断参数是否合法，返回状态码和描述
		if (productId == null || status == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
		}
		// 2、执行修改
		Product product = new Product();
		product.setId(productId);
		product.setStatus(status);
		int resultCount = productMapper.updateByPrimaryKeySelective(product);
		if (resultCount > 0) {
			return ServerResponse.createBySuccessMessage("修改产品状态成功");
		}
		
		return ServerResponse.createByErrorMessage("修改产品状态失败");
	}

	@Override
	public ServerResponse<ProductDetailVo> manageProductDetail(Integer productId) {
        //1、参数非空判断
        if (productId == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
		}
        //2、根据产品ID从数据库中获取数据
        Product product = productMapper.selectByPrimaryKey(productId);
        //3、判断是否存在，不存在说明产品已经下架或者删除了
        if (product == null) {
			return ServerResponse.createByErrorMessage("产品已经下架或者删除");
		}
        //4、引入vo,即表现层对象,对象的属性和页面展示的数据名称一一对应
		ProductDetailVo productDetailVo = assembleProductDetailVo(product);
		return ServerResponse.createBySuccess(productDetailVo);
	}
	
	private ProductDetailVo assembleProductDetailVo(Product product) {
		ProductDetailVo productDetailVo = new ProductDetailVo();
		productDetailVo.setId(product.getId());
        productDetailVo.setSubtitle(product.getSubtitle());
        productDetailVo.setPrice(product.getPrice());
        productDetailVo.setMainImage(product.getMainImage());
        productDetailVo.setSubImages(product.getSubImages());
        productDetailVo.setCategoryId(product.getCategoryId());
        productDetailVo.setDetail(product.getDetail());
        productDetailVo.setName(product.getName());
        productDetailVo.setStatus(product.getStatus());
        productDetailVo.setStock(product.getStock());
		//图片存储在ftp服务器上
        productDetailVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix","http://img.happymmall.com/"));
		
        //根据产品ID来获取产品类型
        Category category = categoryMapper.selectByPrimaryKey(product.getId());
        if (category == null) {
			productDetailVo.setParentCategoryId(0);
		}else {
			productDetailVo.setParentCategoryId(category.getId());
		}
        //使用时间转换工具类进行转换
        productDetailVo.setCreateTime(DateTimeUtil.dateToStr(product.getCreateTime()));
        productDetailVo.setUpdateTime(DateTimeUtil.dateToStr(product.getUpdateTime()));
		return productDetailVo;
	}

	@Override
	public ServerResponse<PageInfo> getProductList(int pageNum, int pageSize) {
		//初始化页面
		PageHelper.startPage(pageNum, pageSize);
		//获取所有产品
		List<Product> productList = productMapper.selectList();
		
		List<ProductListVo> productListVoList = Lists.newArrayList();
		for (Product product : productList) {
			ProductListVo productListVo = assembleProductListVo(product);
			productListVoList.add(productListVo);
		}
		PageInfo pageInfo = new PageInfo(productList);
		pageInfo.setList(productListVoList);
		return ServerResponse.createBySuccess(pageInfo);
	}
	
	private ProductListVo assembleProductListVo(Product product) {
		ProductListVo productListVo = new ProductListVo();
		productListVo.setId(product.getId());
        productListVo.setName(product.getName());
        productListVo.setCategoryId(product.getCategoryId());
        productListVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix","http://img.happymmall.com/"));
        productListVo.setMainImage(product.getMainImage());
        productListVo.setPrice(product.getPrice());
        productListVo.setSubtitle(product.getSubtitle());
        productListVo.setStatus(product.getStatus());
		return productListVo;
	}

	@Override
	public ServerResponse<PageInfo> searchProduct(String productName, Integer productId, int pageNum, int pageSize) {
		//初始化页面
		PageHelper.startPage(pageNum,pageSize);
		//对产品名称进行模糊拼接
		if (StringUtils.isNotBlank(productName)) {
			productName = new StringBuilder().append("%").append(productName).append("%").toString();
		}
		//从数据库中获取数据
		List<Product> productList = productMapper.selectByNameAndProductId(productName,productId);
		
		List<ProductListVo> productListVoList = Lists.newArrayList();
		for (Product product : productList) {
			ProductListVo productListVo = assembleProductListVo(product);
			productListVoList.add(productListVo);
		}
		PageInfo pageInfo = new PageInfo(productList);
		pageInfo.setList(productListVoList);
		return ServerResponse.createBySuccess(pageInfo);
	}

	@Override
	public ServerResponse<ProductDetailVo> getProductDetail(Integer productId) {
		//1、对入参进行非空判断
        if (productId == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
		}
        //2、根据ID从数据库中获取产品信息
        Product product = productMapper.selectByPrimaryKey(productId);
        //3、判断从数据库的获取的数据是否已经下架或者删除，非空和在售状态
        //判断是否删除
        if (product == null) {
			return ServerResponse.createByErrorMessage("商品已经下架或者删除");
		}
        //判断是否下架
        if (product.getStatus() != Const.ProductStatusEnum.ON_SALE.getCode()) {
        	//前端用户不能显示已经下架的商品
			return ServerResponse.createByErrorMessage("商品已经下架或者删除");
		}
        //4、拼装产品详情类用于前端返回
		ProductDetailVo productDetailVo = assembleProductDetailVo(product);
		return ServerResponse.createBySuccess(productDetailVo);
	}
	
	

	/**
	 * todo
	 */
	@Override
	public ServerResponse<PageInfo> getProductByKeywordCategory(String keyword, Integer categoryId, int pageNum,
			int pageSize, String orderBy) {
		
		return null;
	}

}
