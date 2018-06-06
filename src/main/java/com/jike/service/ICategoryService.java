package com.jike.service;

import java.util.List;

import com.jike.common.ServerResponse;
import com.jike.pojo.Category;

public interface ICategoryService {
	/**
	 * 添加分类
	 * @param categoryName
	 * @param parentId
	 * @return
	 */
	ServerResponse addCategory(String categoryName, Integer parentId);
	
	/**
	 * 根据id更新品类名称
	 * @param categoryId
	 * @param categoryName
	 * @return
	 */
    ServerResponse updateCategoryName(Integer categoryId,String categoryName);
    
    /**
     * 根据父品类下的所有子品类
     * 获取品类子节点(平级)
     * @param categoryId
     * @return
     */
    ServerResponse<List<Category>> getChildrenParallelCategory(Integer categoryId);
    
    /**
     * 获取当前分类id及递归子节点categoryId
     * @param categoryId
     * @return
     */
    ServerResponse<List<Integer>> selectCategoryAndChildrenById(Integer categoryId);
}
