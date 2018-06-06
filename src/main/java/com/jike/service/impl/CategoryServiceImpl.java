package com.jike.service.impl;

import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.jike.common.ServerResponse;
import com.jike.dao.CategoryMapper;
import com.jike.dao.UserMapper;
import com.jike.pojo.Category;
import com.jike.service.ICategoryService;

@Service("iCategoryService")
public class CategoryServiceImpl implements ICategoryService {

	private Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);
	
	@Resource
	private CategoryMapper categoryMapper;
	
	@Override
	public ServerResponse addCategory(String categoryName, Integer parentId) {
		//1、对参数进行非空验证
        if (StringUtils.isBlank(categoryName) || parentId == null) {
			
        	return ServerResponse.createByErrorMessage("参数错误");
		}
        //2、实例化Category，设置可用状态
        Category category = new Category();
        category.setParentId(parentId);
        category.setName(categoryName);
        category.setStatus(true);
        //3、添加到数据库
        int count = categoryMapper.insert(category);
        if (count > 0) {
			return ServerResponse.createBySuccessMessage("添加品类成功");
		}
		return ServerResponse.createByErrorMessage("添加品类失败");
	}

	@Override
	public ServerResponse updateCategoryName(Integer categoryId, String categoryName) {
		// 1、对传入的参数进行非空验证
		if (StringUtils.isBlank(categoryName) || categoryId == null) {

			return ServerResponse.createByErrorMessage("参数错误");
		}
		// 2、封装成对象，传入dao层执行更新操作
		Category category = new Category();
		category.setId(categoryId);
		category.setName(categoryName);
		int count = categoryMapper.updateByPrimaryKeySelective(category);
		if (count > 0) {
			return ServerResponse.createBySuccessMessage("更新品类成功");
		}
		return ServerResponse.createByErrorMessage("更新品类失败");
	}

	@Override
	public ServerResponse<List<Category>> getChildrenParallelCategory(Integer categoryId) {
    	//1、根据父品类id获取其下所有子品类
    	List<Category> categoryList = categoryMapper.selectCategoryChildrenByParentId(categoryId);
    	//2、判断是否有子品类，有，则返回数据，没有，返回提示信息
    	if (CollectionUtils.isEmpty(categoryList)) {
			logger.info("未找到当前分类的子分类");
		}
		return ServerResponse.createBySuccess(categoryList);
	}

	@Override
	public ServerResponse<List<Integer>> selectCategoryAndChildrenById(Integer categoryId) {
		//1、通过递归，获取该ID下面的所有子品类
        Set<Category> categorySet = Sets.newHashSet();
        findChildCategory(categorySet, categoryId);
        //2、将所有子品类ID存入集合
        List<Integer> categoryList = Lists.newArrayList();
        if (categoryId != null) {
			for (Category categoryItem : categorySet) {
				categoryList.add(categoryItem.getId());
			}
		}
		return ServerResponse.createBySuccess(categoryList);
	}

	//递归遍历所有子品类
	private Set<Category> findChildCategory(Set<Category> set,Integer categoryId){
		//1、根据ID获取品类，如果存在就存入集合
    	Category category = categoryMapper.selectByPrimaryKey(categoryId);
    	if (category != null) {
			set.add(category);
		}
		//2、再根据这个ID获取其下所有子品类
    	List<Category> list = categoryMapper.selectCategoryChildrenByParentId(categoryId);
		
    	//3、再遍历所有子品类，将品类id存入集合
		for (Category categoryItem : list) {
			findChildCategory(set, categoryItem.getId());
		}
		return set;
	}
	
}
