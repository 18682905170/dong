
/**
 * @标题: JsonUtils.java
 * @包名：  com.saar.gov.util
 * @功能描述：TODO
 * @作者： dongchenhui
 * @创建时间： 2018年12月22日 下午2:34:41
 * @version v1.0
 */

package com.saar.gov.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * @项目名称：gov
 * @包名： com.saar.gov.util
 * @类名称：JsonUtils
 * @类描述：统一输出json
 * @创建人：dongchenhui
 * @创建时间：2018年12月27日下午4:49:37
 * @修改人：dongchenhui
 * @修改时间：2018年12月27日下午4:49:37
 * @修改备注：
 * @version v1.0
 */
@Component
public class ApprovalJsonUtils {
	/**
	 * @方法名: serialize
	 * @描述: 返回Json统一封装方法
	 * @param state
	 * @param msg
	 * @param total
	 * @param data
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2018年12月22日下午4:18:16
	 * @throws
	 */
	public Map<String, Object> serialize(int status, String msg, Integer total, Object data) {
		java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		Map<String, Object> map = Collections.synchronizedMap(new LinkedHashMap<String, Object>());// 使用LinkedHashMap按照插入顺序返回,Collections.synchronizedMap避免多线程不同步
		map.put("code", status);
		map.put("msg", msg);
		map.put("count", total);
		map.put("data", data);
		return map;
	}
}
