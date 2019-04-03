
/**
 * @标题: AuthenticationController.java
 * @包名： com.saar.gov.controller
 * @功能描述：TODO
 * @作者： dongchenhui
 * @创建时间： 2019年1月15日 上午11:02:46
 * @version v1.0
 */

package com.saar.gov.controller;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import com.saar.gov.beans.AuthenticationInf;
import com.saar.gov.service.api.AuthenticationService;
import com.saar.gov.service.api.EnterpriseNameService;
import com.saar.gov.util.DES_Utils;
import com.saar.gov.util.EnterpriseUtil;
import com.saar.gov.util.JsonUtils;
import com.saar.gov.util.PropertyUtil;
import com.saar.gov.util.ValidatorUtils;
import com.saar.gov.util.WaterMarkGenerate;

/**
 * @项目名称：gov
 * @包名： com.saar.gov.controller
 * @类名称：AuthenticationController
 * @类描述：用户个人实名认证
 * @创建人：dongchenhui
 * @创建时间：2019年1月15日上午11:02:46
 * @修改人：dongchenhui
 * @修改时间：2019年1月15日上午11:02:46
 * @修改备注：
 * @version v1.0
 */

@Controller
@RequestMapping("/samr")
public class AuthenticationController {
	@Autowired
	AuthenticationService authenticationServiceImpl;
	
	@Autowired
	EnterpriseNameService enterpriseNameServiceImpl;
	
	@Autowired
	EnterpriseUtil enterpriseUtil;
	
	@Autowired
	JsonUtils jsonUtils;
	
	@Autowired
	DES_Utils desutil;// DES加密工具
	
	/**
	 * @方法名: authenticationJudge
	 * @描述: 判断用户是否已进行实名认证
	 * @param OPENID 用户在当前微信公众号的身份id
	 * @param state 判断用户从哪里点击进来的
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月15日下午1:09:58
	 * @throws
	 */
	@RequestMapping("/smrz")
	@ResponseBody		
	public Map<String, Object> authenticationJudge(@RequestParam("OPENID") String OPENID,@RequestParam(value="state",required=false) String state){
		int status = 0;
		Map<String, Object> map = authenticationServiceImpl.selectRecord(OPENID);
		if(map == null) {//未实名认证
			Map<String, Object> data = new HashMap<>();
			AuthenticationInf authenticationInf = new AuthenticationInf();
			String BH = enterpriseUtil.generateCode();
			authenticationInf.setBh(BH);
			authenticationInf.setOpenid(OPENID);
			SimpleDateFormat aDate = new SimpleDateFormat("yyyyMMddHHmmss");
			String content = BH+","+aDate.format(new Date());
			String token = desutil.encryptBasedDes(content);
			authenticationInf.setLogin_token(token);
		
			status = authenticationServiceImpl.insertAuthentication(authenticationInf);
			
			data.put("TOKEN", token);
			data.put("SMRZ_ZT", "1");
			data.put("flagFull", "0");//flagFull，该用户有已办理完成的业务，0代表没有，1代表有
			data.put("flagHalf", "0");//flagHalf，该用户有未办理完成的业务，0代表没有，1代表有
			
			return jsonUtils.serialize(status, "", 0, data);
		}else {//已实名认证或者实名认证还未完成
			Map<String, Object> data = new HashMap<>();
			data.put("flagFull", "0");
			data.put("flagHalf", "0");
			if(map.get("SFZ_HM") != null) {
			List<Map<String,Object>> BH_Full = enterpriseNameServiceImpl.selectEnterpriseNameBHCompleted(map.get("SFZ_HM").toString());
			if(BH_Full.size()>0) {
				data.put("flagFull", "1");
			}
			
			List<Map<String,Object>> BH_Half = enterpriseNameServiceImpl.selectEnterpriseNameBHUnCompleted(map.get("SFZ_HM").toString());
			
			if(BH_Half.size()>0) {
				data.put("flagHalf", "1");
			}
			}
			if(!"mchz".equals(state)) {//当点击设立业务大厅中的设立登记进来的时候
			SimpleDateFormat aDate = new SimpleDateFormat("yyyyMMddHHmmss");
			String content = null;
			if("0".equals(map.get("SMRZ_ZT"))) {//如果实名认证完成，用身份证号码生成token
				content = map.get("SFZ_HM")+","+map.get("BH")+","+aDate.format(new Date());
			}else {//如果实名认证未完成，用用户编号生成token
				content = map.get("BH")+","+aDate.format(new Date());
			}
			
			String token = desutil.encryptBasedDes(content);		
			AuthenticationInf authenticationInf = new AuthenticationInf();
			authenticationInf.setBh(map.get("BH").toString());
			authenticationInf.setLogin_token(token);
			status = authenticationServiceImpl.updateEnterpriseName(authenticationInf);
				
			data.put("TOKEN", token);
			}
			data.put("SMRZ_ZT", map.get("SMRZ_ZT"));
			return jsonUtils.serialize(1, "", 0, data);
		}
	}
	
	
	/**
	 * @方法名: identifyCodeConfirm
	 * @描述:  提交身份证姓名、性别、住址等信息
	 * @param token 用户身份认证令牌
	 * @param ZZ 身份证住址
	 * @param XM 身份证姓名
	 * @param SFZ_HM 身份证号码
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月15日下午1:42:20
	 * @throws
	 */
	@RequestMapping("/mchz/sfzxx")
	@ResponseBody
	public Map<String, Object> idCardConfirm(@RequestHeader("token") String token,@RequestParam("ZZ") String ZZ,
												   @RequestParam("XM") String XM,@RequestParam("SFZ_HM") String SFZ_HM){
		//校验投资人证件类型(身份证或统一社会信用代码)
		if(!ValidatorUtils.validator(SFZ_HM)) return jsonUtils.serialize(0, "居民身份证或有误", 0,"");
		if(!ValidatorUtils.isChinese(XM)) return jsonUtils.serialize(0, "姓名含非中文字符", 0,"");
		if(XM.getBytes().length>50) return jsonUtils.serialize(0, "投资人姓名长度超出", 0,"");
		
		String a = authenticationServiceImpl.selectOpenId(SFZ_HM);
		if(authenticationServiceImpl.selectOpenId(SFZ_HM) != null) {
			return jsonUtils.serialize(2, "该用户已注册！", 0, "");
		}
		int status = 0;
		String content = desutil.decryptBasedDes(token);
		String[] arr = content.split(",");
		String ZCYH_BH = arr[0];
		AuthenticationInf authenticationInf = new AuthenticationInf();
		authenticationInf.setBh(ZCYH_BH);
		authenticationInf.setZz(ZZ);
		authenticationInf.setXm(XM);
		authenticationInf.setSfz_hm(SFZ_HM);
		authenticationInf.setSmrz_zt("0");
		authenticationInf.setSmrz_wcrq(new Date());
		status = authenticationServiceImpl.updateEnterpriseName(authenticationInf);
		Map<String, Object> data = new HashMap<>();
		data.put("ZCYH_BH", ZCYH_BH);
		return jsonUtils.serialize(status, "", 0, data);
	}
	
	
	/**
	 * @方法名: sfzmzsc
	 * @描述:  身份证正面上传
	 * @param file_zm 身份证正面照片
	 * @param token 用户身份认证令牌
	 * @param response
	 * @param request
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月15日下午1:46:21
	 * @throws
	 */
	@RequestMapping("/sfzzmsc")
	@ResponseBody
	public Map<String, Object> idCardFront(@RequestParam(value="file_zm",required=false)CommonsMultipartFile file_zm,String token,HttpServletResponse response ,HttpServletRequest request) {
		int status = 0;
		String content = desutil.decryptBasedDes(token);
		String[] arr = content.split(",");
		String ZCYH_BH = arr[0];
		String path = savaImg(file_zm, request, ZCYH_BH, "zm");
		AuthenticationInf authenticationInf = new AuthenticationInf();
		authenticationInf.setBh(ZCYH_BH);
		authenticationInf.setSfz_zm(path);
		status = authenticationServiceImpl.updateEnterpriseName(authenticationInf);
		Map<String, Object> data = new HashMap<>();
		data.put("SFZ_LJ", path);

		return jsonUtils.serialize(status, "", 0, data);
	}
	
	/**
	 * @方法名: idCardBack
	 * @描述:  身份证背面上传
	 * @param file_zm 身份证正面照片
	 * @param token 用户身份认证令牌
	 * @param response
	 * @param request
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月15日下午1:55:58
	 * @throws
	 */
	@RequestMapping("/sfzbmsc")
	@ResponseBody
	public Map<String, Object> idCardBack(@RequestParam(value="file_fm",required=false)CommonsMultipartFile file_fm,String token,HttpServletResponse response ,HttpServletRequest request) {
		int status = 0;
		String content = desutil.decryptBasedDes(token);
		String[] arr = content.split(",");
		String ZCYH_BH = arr[0];
		String path = savaImg(file_fm, request, ZCYH_BH, "bm");
		AuthenticationInf authenticationInf = new AuthenticationInf();
		authenticationInf.setBh(ZCYH_BH);
		authenticationInf.setSfz_bm(path);
		status = authenticationServiceImpl.updateEnterpriseName(authenticationInf);
		Map<String, Object> data = new HashMap<>();
		data.put("SFZ_LJ", path);

		return jsonUtils.serialize(status, "", 0, data);
	}
	
	
	/**
	 * @方法名: rzzl
	 * @描述:  预览
	 * @param token 用户身份认证令牌
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月15日下午2:38:56
	 * @throws
	 */
	@RequestMapping("/mchz/rzzl")
	@ResponseBody
	public Map<String, Object> rzzl(@RequestHeader("token") String token) {
		String content = desutil.decryptBasedDes(token);
		String[] arr = content.split(",");
		String ZCYH_BH = arr[0];
		int status = 0;
		AuthenticationInf authenticationInf = authenticationServiceImpl.selectAuthenticationInfByID(ZCYH_BH);
		if(authenticationInf != null) {
			status = 1;
		}
		
		return jsonUtils.serialize(status, "", 0, authenticationInf);
	}
		
	
	/**
	 * @方法名: savaImg
	 * @描述:  上传图片
	 * @param file 被上传的图片对象
	 * @param request
	 * @param BH 身份证号码
	 * @param flag
	 * @return
	 * @返回类型 String
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月15日下午1:53:03
	 * @throws
	 */
	private String savaImg(CommonsMultipartFile file,HttpServletRequest request,String BH,String flag) {
		String fileName =file.getOriginalFilename();
		System.out.println("fileName======"+fileName);
		//读取配置文件获取身份证上传路径
		String path=PropertyUtil.loadProps("config/fileconfig.properties", "sfzimg");
		String extensionname = fileName.substring(fileName.lastIndexOf(".") + 1);
		String fileNames = BH+"_"+flag; 
		String newFilename = fileNames + "." + extensionname;
		String savePath=path+newFilename;
		try {
			file.transferTo(new File(savePath));
			File tarFile=new File(path,newFilename);
			WaterMarkGenerate.generateWithTextMark(tarFile, path+"logo_"+newFilename, "与原件一致！");
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	    
		return "/sfzimages/"+"logo_"+newFilename;
	}
	
}
