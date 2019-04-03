package com.saar.gov.interceptor;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.saar.gov.beans.AuthenticationInf;
import com.saar.gov.service.api.AuthenticationService;
import com.saar.gov.service.api.MenueInfService;
import com.saar.gov.service.api.UserInfService;
import com.saar.gov.service.impl.AuthenticationServiceImpl;
import com.saar.gov.util.DES_Utils;

import net.sf.json.JSONObject;

/**
 * @项目名称：gov
 * @包名： com.saar.gov.interceptor
 * @类名称：TokenInterceptor
 * @类描述：自定义拦截器，在用户登陆过的每次请求进行Token的验证
 * @创建人：dongchenhui
 * @创建时间：2018年12月19日上午11:41:58
 * @修改人：dongchenhui
 * @修改时间：2018年12月19日上午11:41:58
 * @修改备注：
 * @version v1.0
 */
public class TokenMCHZInterceptor implements HandlerInterceptor {
	
	@Autowired
	DES_Utils desutil;// DES加密工具
	
	@Autowired
	AuthenticationService authenticationServiceImpl;
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		JSONObject jso = new JSONObject();
		response.setCharacterEncoding("UTF-8");
		DES_Utils desutil = new DES_Utils();
		String token = request.getHeader("token");	
		if(token != null && !"".equals(token)) {
			String content = desutil.decryptBasedDes(token);
			String[] arr = content.split(",");
			String ZCYH_BH = arr[0];
			int a = arr.length;
			if(arr.length > 2) {
				ZCYH_BH = arr[1];
			}
			
			AuthenticationInf af = authenticationServiceImpl.selectAuthenticationInfByID(ZCYH_BH);
			String token1 = af.getLogin_token();
			if(token.equals(af.getLogin_token())) {
				return true;
			}else {
				PrintWriter out = response.getWriter();
				jso.put("status", "0");
				jso.put("errorMessage", "非法Token！");
				out.print(jso.toString());//返回验证错误信息
				out.flush();
				out.close();
			}
		}else {//token不存在
			PrintWriter out = response.getWriter();
			jso.put("status", "0");
			jso.put("errorMessage", "Token不存在！");
			out.print(jso.toString());//返回验证错误信息
			out.flush();
			out.close();
			return false;
		}
		return false;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		// TODO Auto-generated method stub

	}

}
