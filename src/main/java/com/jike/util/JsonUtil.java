package com.jike.util;

import static org.hamcrest.CoreMatchers.instanceOf;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.type.TypeReference;

import com.google.common.collect.Lists;
import com.jike.pojo.User;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonUtil {

	private static ObjectMapper objectMapper = new ObjectMapper();
	
	
	static {
		//将对象的所有字段列入Json序列化中
		objectMapper.setSerializationInclusion(Inclusion.ALWAYS);
		//取消默认转换timestamps形式
		objectMapper.configure(SerializationConfig.Feature.WRITE_DATE_KEYS_AS_TIMESTAMPS, false);
		//忽略空Bean转json的错误
		objectMapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
		//统一日期格式
		objectMapper.setDateFormat(new SimpleDateFormat(DateTimeUtil.STANDARD_FORMAT));
		//忽略    在json字符创中存在，但是在java对象中不存在对应属性的情况，防止错误，在前端跟后台由于某种原因，字段不对应而又无关紧要的时候，为了防止后台在进行反序列化时报错，需这样设置
		objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	//将对象转换为json字符串（序列化）
	public static <T> String obj2String(T object) {
		if (object == null) {
			return null;
		}
		try {
			return object instanceof String ? (String)object : objectMapper.writeValueAsString(object);
		} catch (Exception e) {
			log.warn("Parse Object to String error",e);
			return null;
		} 
	}
	
	//将对象转换为格式化的json字符串（序列化）
	public static <T> String obj2StringPretty(T object) {
		if (object == null) {
			return null;
		}
		try {
			return object instanceof String ? (String)object : objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
		} catch (Exception e) {
			log.warn("Parse Object to String error",e);
			return null;
		} 
	}
	
	//将json字符串转化为对象（反序列化）------字符串是由普通对象序列化而来的
	public static <T> T string2Object(String str,Class<T> clazz) {
		if (StringUtils.isEmpty(str) || clazz == null) {
			return null;
		}
		try {
			return clazz.equals(String.class) ? (T)str : objectMapper.readValue(str, clazz);
		} catch (Exception e) {
			log.warn("Parse String to Object error",e);
			return null;
		}
	}
	//当字符串是由泛型集合转换来的时候，得使用这种方法  List<User>
	public static <T> T string2Object(String str,TypeReference<T> typeReference) {
		if (StringUtils.isEmpty(str) || typeReference == null) {
			return null;
		}
		try {
			return (T)(typeReference.getType().equals(String.class)? str : objectMapper.readValue(str, typeReference));
		} catch (Exception e) {
			log.warn("Parse String to Object error",e);
			return null;
		}
	}
	
	
	public static void main(String[] args) {
		User user1 = new User();
		user1.setId(1);
		user1.setEmail("1944@qq.com");
		
		User user2 = new User();
		user2.setId(2);
		user2.setEmail("19444@qq.com");
		
		List<User> userList = Lists.newArrayList();
		userList.add(user1);
		userList.add(user2);
		
		String userListStr = JsonUtil.obj2StringPretty(userList);
		log.info(userListStr);
		
		//List<User> userList2 = JsonUtil.string2Object(userListStr, List.class);
		List<User> userList2 = JsonUtil.string2Object(userListStr, new TypeReference<List<User>>() {});
		
		/*String str = JsonUtil.obj2String(user);
		log.info("test object2json:{}",str);
		log.info("test object2jsonpretty{}",JsonUtil.obj2StringPretty(user));*/
		
		//User user2 = JsonUtil.string2Object(str, User.class);
		
		System.out.println("end");
	}
	
}
