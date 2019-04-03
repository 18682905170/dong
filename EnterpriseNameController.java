
/**
 * @标题: EnterpriseNameController.java
 * @包名： com.saar.gov.controller
 * @功能描述：TODO
 * @作者： dongchenhui
 * @创建时间： 2018年12月28日 下午2:42:07
 * @version v1.0
 */

package com.saar.gov.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import com.github.pagehelper.util.StringUtil;
import com.saar.gov.beans.EnterpriseInf;
import com.saar.gov.beans.InvestorInf;
import com.saar.gov.beans.SczrJyfwJbxx;
import com.saar.gov.beans.SegmentProgressInf;
import com.saar.gov.constants.CouldApprovalConstants;
import com.saar.gov.service.api.AuthenticationService;
import com.saar.gov.service.api.EnterpriseNameService;
import com.saar.gov.service.api.InvestorInfService;
import com.saar.gov.service.api.SczrJyfwJbxxService;
import com.saar.gov.service.api.SegmentProgressService;
import com.saar.gov.service.api.SysJxycService;
import com.saar.gov.util.DES_Utils;
import com.saar.gov.util.EnterpriseUtil;
import com.saar.gov.util.JsonUtils;
import com.saar.gov.util.PropertyUtil;
import com.saar.gov.util.ValidatorUtils;
import com.saar.gov.util.WordTemplate;
import com.wordnik.swagger.annotations.ApiOperation;

import io.swagger.annotations.Api;
import sun.misc.BASE64Decoder;
/**
 * @项目名称：gov
 * @包名： com.saar.gov.controller
 * @类名称：EnterpriseNameController
 * @类描述：处理企业名称预先核准阶段相关功能
 * @创建人：dongchenhui
 * @创建时间：2018年12月28日下午2:42:07
 * @修改人：dongchenhui
 * @修改时间：2018年12月28日下午2:42:07
 * @修改备注：
 * @version v1.0
 */
@Controller
@RequestMapping("/samr")
@Api(value = "/samr", tags = "企业名称核准接口")
public class EnterpriseNameController {
	private static final Logger logger = LoggerFactory.getLogger(EnterpriseNameController.class);
	@Autowired
	JsonUtils jsonUtils;  
	
	@Autowired
	EnterpriseNameService enterpriseNameServiceImpl;
	
	@Autowired
	SegmentProgressService segmentProgressServiceImpl;
	
	@Autowired
	InvestorInfService investorInfServiceImpl;
	
	@Autowired
	EnterpriseUtil enterpriseUtil;
	
	@Autowired
	DES_Utils desutil;// DES加密工具
	
	@Autowired
	AuthenticationService authenticationServiceImpl;
	
	@Autowired
	private SysJxycService sysJxycService;
	
	@Autowired
	private SczrJyfwJbxxService sczrJyfwJbxxService;
		
	/**
	 * @方法名: enterpriseName
	 * @描述:	  企业名称相关字段入库
	 * @param XZQH 行政区划
	 * @param QYZH 企业字号
	 * @param HYTD 行业特点
	 * @param ZZXS 组织形式
	 * @param BH
	 * @return availabilityStatus 企业 名称字段是否可用 :0可用,1包含禁止性用词,2包含限制性用词,3包含禁止性用词与限制性用词,4有风险
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2018年12月29日上午11:45:48
	 * @throws
	 */
	@RequestMapping("/mchz/qymcqr")
	@ResponseBody
	@ApiOperation(value = "企业名称相关字段入库", notes = "企业名称相关字段入库")
	public Map<String, Object> enterpriseName(@RequestParam("XZQH") String XZQH,@RequestParam("QYZH") String QYZH,
											  @RequestParam("HYTD") String HYTD,@RequestParam("ZZXS") String ZZXS,
											  @RequestHeader("token") String token,@RequestParam(value="MCHZ_BH",required=false) String MCHZ_BH
											  ){
		logger.info("==========enterpriseName(String XZQH,String QYZH,String HYTD,String ZZXS,String ZCYH_BH,String MCHZ_BH,String flag)===========");
		
		//判断企业字号和行业特点,组织形式字节长度与是否包含非中午字符
		if(QYZH.getBytes().length>20) return jsonUtils.serialize(0, "企业字号长度超出", 0,"");
		if(HYTD.getBytes().length>20) return jsonUtils.serialize(0, "行业特点长度超出", 0,"");
		if(!ValidatorUtils.isChinese(QYZH)) return jsonUtils.serialize(0, "企业字号包含非中文字符", 0,"");
		if(!ValidatorUtils.isChinese(HYTD)) return jsonUtils.serialize(0, "行业特点包含非中文字符", 0,"");
		if(!ValidatorUtils.isChinese(ZZXS)) return jsonUtils.serialize(0, "组织形式包含非中文字符", 0,"");
		
		Map<String, Object> data = new HashMap<>();
		int availabilityStatus = 0;
		
		//判断企业字号是否包含禁限用关键词
		Map<String,Object> mapSelectQyzh = selectRestrictedByKeyWord(QYZH);
		if(mapSelectQyzh.get("availabilityStatus")!=null) {
			data.put("availabilityStatus", mapSelectQyzh.get("availabilityStatus"));
			data.put("sysJxycList", mapSelectQyzh.get("sysJxycList"));
			return jsonUtils.serialize(1, "企业字号"+mapSelectQyzh.get("message").toString(), 0, data);
		}
		//判断行业特点是否包含禁限用关键词
		Map<String,Object> mapSelectHytd = selectRestrictedByKeyWord(HYTD);
		if(mapSelectHytd.get("availabilityStatus")!=null) {
			data.put("availabilityStatus", mapSelectHytd.get("availabilityStatus"));
			data.put("sysJxycList", mapSelectHytd.get("sysJxycList"));
			return jsonUtils.serialize(1, "行业特点"+mapSelectHytd.get("message").toString(), 0, data);
		}
		//判断企业名称是否包含禁限用关键词
		String keyWord = XZQH + QYZH + HYTD + ZZXS;
		Map<String,Object> mapSelectKeyWord = selectRestrictedByKeyWord(keyWord);
		if(mapSelectKeyWord.get("availabilityStatus")!=null) {
			data.put("availabilityStatus", mapSelectKeyWord.get("availabilityStatus"));
			data.put("sysJxycList", mapSelectKeyWord.get("sysJxycList"));
			return jsonUtils.serialize(1, "企业名称"+mapSelectKeyWord.get("message").toString(), 0, data);
		}
		
		//解析token，获取用户身份证号码
		String content = desutil.decryptBasedDes(token);
		String[] arr = content.split(",");
		String YHSFZJHM = arr[0];
		//构造名称核准业务对象
		EnterpriseInf ef = new EnterpriseInf();
		//构造业务进程对象
		SegmentProgressInf sf = new SegmentProgressInf();
		//状态
		int status = 0;
		//状态信息
		String msg = "操作失败！";
		//数据总条数
		int total = 0;
		if("".equals(MCHZ_BH)) {//第一次新办进来的
			//生成18位编码
			String ID = enterpriseUtil.generateCode();
			String ID_YWJC = enterpriseUtil.generateCode();
			ef.setBh(ID);
			ef.setYhsfzjhm(YHSFZJHM);
			ef.setXzqh(XZQH);
			ef.setQyzh(QYZH);
			ef.setHytd(HYTD);
			ef.setZzxs(ZZXS);
			ef.setDjrq(new Date());
			status = enterpriseNameServiceImpl.insertEnterpriseName(ef);
			//构造业务进程对象，存储当前业务办理的进度相关信息
			sf = getSegmentProgressInfRelated(ID_YWJC, CouldApprovalConstants.YWLX_ENTERPRISENAME, ID, CouldApprovalConstants.YWJZ_NAME, CouldApprovalConstants.YWGX_RELATED_ME, CouldApprovalConstants.CZDZ_NAME, YHSFZJHM,"2");
			segmentProgressServiceImpl.insertSegmentProgress(sf);
			data.put("MCHZ_BH", ID);
			data.put("YWJC_BH", ID_YWJC);//业务进程编号
			data.put("availabilityStatus", availabilityStatus);
			System.out.println("插入成功！");
			msg = "成功！";
		}else if(!"".equals(MCHZ_BH)) {//点击上一步过来的
			validateMchzBhAndToken(MCHZ_BH, token);
			ef.setBh(MCHZ_BH);
			ef.setXzqh(XZQH);
			ef.setQyzh(QYZH);
			ef.setHytd(HYTD);
			ef.setZzxs(ZZXS);
			ef.setDjrq(new Date());
			status = enterpriseNameServiceImpl.updateEnterpriseName(ef);
			Map<String,Object> map = segmentProgressServiceImpl.selectYWJCBH(MCHZ_BH, YHSFZJHM);
			String YWJC_BH = map.get("BH").toString();
			data.put("YWJC_BH", YWJC_BH);//业务进程编号
			data.put("MCHZ_BH", MCHZ_BH);
			data.put("availabilityStatus", availabilityStatus);
			System.out.println("插入成功！");
			msg = "成功！";
		}else {
			throw new RuntimeException();
		}
		return jsonUtils.serialize(status, msg, total, data);
	}
	
	/**
	 * @方法名:  selectRestrictedByKeyWord
	 * @描述:   查询禁限用表数据返回状态与信息内部方法
	 * @param  keyWord  企业字号
	 * @return availabilityStatus 企业 名称字段是否可用 :0可用,1包含禁止性用词,2包含限制性用词,3包含禁止性用词与限制性用词,4有风险
	 * @返回类型  Map<String,Object>
	 * @创建人   djw
	 * @创建时间  2019年2月20日上午09:25:07
	 * @throws
	 */
	private Map<String, Object> selectRestrictedByKeyWord(String keyWord){
		Map<String,Object> data =  new HashMap<>();
		List<Map<String,Object>> sysJxycList = new ArrayList<>();
		sysJxycList = sysJxycService.selectSysJxycBykeyWord(keyWord);
		if(sysJxycList.size() == 1 && "1".equals(sysJxycList.get(0).get("GJCLX"))) {
			data.put("availabilityStatus", 1);
			data.put("sysJxycList", sysJxycList);
			data.put("message", "包含禁止性用词!");
		}else if(sysJxycList.size() == 1 && "2".equals(sysJxycList.get(0).get("GJCLX"))) {
			data.put("availabilityStatus", 2);
			data.put("sysJxycList", sysJxycList);
			data.put("message", "包含限制性用词!");
		}else if(sysJxycList.size() > 1){
			if(!sysJxycList.get(0).get("GJCLX").equals(sysJxycList.get(sysJxycList.size()-1).get("GJCLX"))) {
				data.put("availabilityStatus", 3);
				data.put("sysJxycList", sysJxycList);
				data.put("message", "包含禁止性和限制性用词!");
			}else if(sysJxycList.get(0).get("GJCLX").equals("1")){
				data.put("availabilityStatus", 1);
				data.put("sysJxycList", sysJxycList);
				data.put("message", "包含禁止性用词!");
			}else {
				data.put("availabilityStatus", 2);
				data.put("sysJxycList", sysJxycList);
				data.put("message", "包含限制性用词!");
			}
		}
		return data;
	}
	
	/**
	 * @方法名: enterpriseLocation
	 * @描述:  更新企业如住所地
	 * @param QYZS_SHENG 企业住所-省(市/自治州)
	 * @param QYZS_SHI 企业住所-市（地区/盟/自治州）
	 * @param QYZS_XIAN 企业住所-县(自治县/旗/自治旗/市/区)
	 * @param QYZS_XIANG 企业住所-乡（民族乡/镇/街道）
	 * @param QYZS_CUN 企业住所-村（路/社区）
	 * @param QYZS_HAO 企业住所-号 详细地址（门牌）
	 * @param MCHZ_BH 业务流水号
	 * @return YWJC_BH 业务进程编号
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2018年12月29日上午11:46:22
	 * @throws
	 */
	@RequestMapping("/qyzsdqr")
	@ResponseBody
	public Map<String, Object> enterpriseLocation(@RequestParam("QYZS_SHENG") String QYZS_SHENG,@RequestParam("QYZS_SHI") String QYZS_SHI,
											  @RequestParam("QYZS_XIAN") String QYZS_XIAN,@RequestParam("QYZS_XIANG") String QYZS_XIANG,
											  @RequestParam("QYZS_CUN") String QYZS_CUN,@RequestParam("QYZS_HAO") String QYZS_HAO,@RequestParam("MCHZ_BH") String MCHZ_BH,@RequestParam("YWJC_BH") String YWJC_BH){
		logger.info("==========enterpriseLocation(String QYZS_SHENG,String QYZS_SHI,String QYZS_XIAN,String QYZS_XIANG,String QYZS_CUN,String QYZS_HAO,String MCHZ_BH)===========");
		
		//判断企业住所地字节长度与是否包含特殊字符
		if(QYZS_SHENG.getBytes().length>20) return jsonUtils.serialize(0, "企业住所-省(市/自治州)长度超出", 0,"");
		if(QYZS_SHI.getBytes().length>20) return jsonUtils.serialize(0, "企业住所-市（地区/盟/自治州）长度超出", 0,"");
		if(QYZS_XIAN.getBytes().length>20) return jsonUtils.serialize(0, "企业住所-县(自治县/旗/自治旗/市/区)长度超出", 0,"");
		if(QYZS_XIANG.getBytes().length>20) return jsonUtils.serialize(0, "企业住所-乡（民族乡/镇/街道）长度超出", 0,"");
		if(QYZS_CUN.getBytes().length>20) return jsonUtils.serialize(0, "企业住所-村（路/社区）长度超出", 0,"");
		if(QYZS_HAO.getBytes().length>50) return jsonUtils.serialize(0, "企业住所-号 详细地址（门牌）长度超出", 0,"");
		
		if(ValidatorUtils.isSpecialChar(QYZS_SHENG)) return jsonUtils.serialize(0, "企业住所-省(市/自治州)包含特殊字符", 0,"");
		if(ValidatorUtils.isSpecialChar(QYZS_SHI)) return jsonUtils.serialize(0, "企业住所-市（地区/盟/自治州）包含特殊字符", 0,"");
		if(ValidatorUtils.isSpecialChar(QYZS_XIAN)) return jsonUtils.serialize(0, "企业住所-县(自治县/旗/自治旗/市/区)包含特殊字符", 0,"");
		if(ValidatorUtils.isSpecialChar(QYZS_XIANG)) return jsonUtils.serialize(0, "企业住所-乡（民族乡/镇/街道）包含特殊字符", 0,"");
		if(ValidatorUtils.isSpecialChar(QYZS_CUN)) return jsonUtils.serialize(0, "企业住所-村（路/社区）包含特殊字符", 0,"");
		if(ValidatorUtils.isSpecialChar(QYZS_HAO)) return jsonUtils.serialize(0, "企业住所-号 详细地址（门牌）包含特殊字符", 0,"");
		
		validateMchzBh(MCHZ_BH);
		Map<String, Object> data = new HashMap<>();
		//状态
		int status = 0;
		//状态信息
		String msg = "失败！";
		//数据总条数
		int total = 0;
		//构造业务进程对象
		SegmentProgressInf sf = new SegmentProgressInf();
		//构造名称核准业务对象
		EnterpriseInf ef = new EnterpriseInf();
		ef.setBh(MCHZ_BH);
		ef.setQyzs_sheng(QYZS_SHENG);
		ef.setQyzs_shi(QYZS_SHI);
		ef.setQyzs_xian(QYZS_XIAN);
		ef.setQyzs_xiang(QYZS_XIANG);
		ef.setQyzs_cun(QYZS_CUN);
		ef.setQyzs_hao(QYZS_HAO);
		
		status=enterpriseNameServiceImpl.updateEnterpriseName(ef);
		//构造业务进程对象，存储当前业务办理的进度相关信息
		sf = getSegmentProgressInfRelated(YWJC_BH, CouldApprovalConstants.YWLX_ENTERPRISENAME, MCHZ_BH, CouldApprovalConstants.YWJZ_LOCATION, CouldApprovalConstants.YWGX_RELATED_ME, CouldApprovalConstants.CZDZ_LOCATION, null, null);
		segmentProgressServiceImpl.updateSegmentProgress(sf);
		
		data.put("MCHZ_BH", MCHZ_BH);
		data.put("YWJC_BH", YWJC_BH);//业务进程编号
		msg = "成功！";
		return jsonUtils.serialize(status, msg, total, data);
	}
		
	/**
	 * @方法名: investorInf
	 * @描述: 投资人信息入库
	 * @param TZR_LX 投资人类型（自然人或法人）
	 * @param TZR_XM 投资人姓名（自然人名称或企业名称）
	 * @param TZR_ZZHM 投资人证照号码（自然人身份证或企业统一社会信用代码）
	 * @param MCHZ_BH 业务流水号
	 * @return
	 * @返回类型 InvestorInf
	 * @创建人 dongchenhui
	 * @创建时间 2018年12月29日下午4:27:00
	 * @throws
	 */
	@SuppressWarnings("unused")
	@RequestMapping("/tzrxx")
	@ResponseBody
	public Map<String, Object> investorInf(@RequestParam("TZR_ZJLX") String TZR_ZJLX,@RequestParam("TZR_LX") String TZR_LX,@RequestParam("TZR_XM") String TZR_XM,
											  @RequestParam("TZR_ZZHM") String TZR_ZZHM,@RequestParam("MCHZ_BH") String MCHZ_BH){
		logger.info("==========investorInf(String TZR_LX,String TZR_XM,String TZR_ZZHM,String MCHZ_BH)===========");
		
		//校验投资人证件类型(身份证或统一社会信用代码)
		if(TZR_ZJLX.equals("居民身份证") && (!ValidatorUtils.validator(TZR_ZZHM))) return jsonUtils.serialize(0, "居民身份证号码有误", 0,"");
		if(TZR_ZJLX.equals("统一社会信用代码") && (!ValidatorUtils.isLicense18(TZR_ZZHM))) return jsonUtils.serialize(0, "统一社会信用代码有误", 0,"");
		if(!ValidatorUtils.isChinese(TZR_XM)) return jsonUtils.serialize(0, "姓名含非中文字符", 0,"");
		if(TZR_XM.getBytes().length>30) return jsonUtils.serialize(0, "投资人姓名长度超出", 0,"");
		
		validateMchzBh(MCHZ_BH);
		InvestorInf tf = new InvestorInf();
		//状态
		int status = 0;
		//状态信息
		String msg = "失败！";
		//数据总条数
		int total = 0;
		//生成18位编码
		String ID = enterpriseUtil.generateCode();
		try {
			//判断身份证是否已存在,存在则不重复添加
			tf = investorInfServiceImpl.selectInvestorByTzrZzhmAndMchzBh(TZR_ZZHM, MCHZ_BH);
			if(tf!=null) {
				status=0;
				msg = "该投资人已添加！";
			}else {
				InvestorInf tf1 = new InvestorInf();
				tf1.setBh(ID);
				tf1.setMchz_bh(MCHZ_BH);
				tf1.setTzr_lx(TZR_LX);
				tf1.setTzr_zjlx(TZR_ZJLX);
				tf1.setTzr_xm(TZR_XM);
				tf1.setTzr_zzhm(TZR_ZZHM);
				tf1.setDjrq(new Date());
				status=investorInfServiceImpl.insertInvestorInf(tf1);
				msg = "添加成功！";
				tf = tf1;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonUtils.serialize(status, msg, total, tf);
	}
		
	/**
	 * @方法名: enterpriseInvestors
	 * @描述: 投资信息确认
	 * @param ZCZB 注册资本
	 * @param QYLX 企业类型
	 * @param MCHZ_BH 业务流水号 
	 * @param YWJC_BH 业务进程编号
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2018年12月29日下午4:26:36
	 * @throws
	 */
	@RequestMapping("/tzxxqr")
	@ResponseBody
	public Map<String, Object> enterpriseInvestors(@RequestParam("ZCZB") String ZCZB,@RequestParam("QYLX") String QYLX,
											  @RequestParam("MCHZ_BH") String MCHZ_BH,@RequestParam("YWJC_BH") String YWJC_BH){
		logger.info("==========enterpriseInvestors(String ZCZB,String QYLX,String MCHZ_BH)===========");
		
		//判断注册资本是否为数字， 最多支持小数点后2位
		if (!ValidatorUtils.isNumeric(ZCZB)) return jsonUtils.serialize(0, "注册资本非数字或小数", 0,"");
		if (ZCZB.contains(".") && ZCZB.split("\\.")[1].length()>2) return jsonUtils.serialize(0, "注册资本小数位大于2位", 0,"");
		
		validateMchzBh(MCHZ_BH);
		Map<String, Object> data = new HashMap<>();
		//状态
		int status = 0;
		//状态信息
		String msg = "失败！";
		//数据总条数
		int total = 0;
		EnterpriseInf ef = new EnterpriseInf();
		ef.setBh(MCHZ_BH);
		ef.setZczb(ZCZB);
		ef.setQylx(QYLX);		
		status=enterpriseNameServiceImpl.updateEnterpriseName(ef);
		SegmentProgressInf sf = new SegmentProgressInf();
		//构造业务进程对象，存储当前业务办理的进度相关信息
		sf = getSegmentProgressInfRelated(YWJC_BH, CouldApprovalConstants.YWLX_ENTERPRISENAME, MCHZ_BH, CouldApprovalConstants.YWJZ_INVESTORS, CouldApprovalConstants.YWGX_RELATED_ME, CouldApprovalConstants.CZDZ_INVESTORS, null, null);
		segmentProgressServiceImpl.updateSegmentProgress(sf);
		data.put("YWJC_BH", YWJC_BH);//业务进程编号
		data.put("MCHZ_BH", MCHZ_BH);
		msg = "成功！";
		return jsonUtils.serialize(status, msg, total, data);
	}
		
	/**
	 * @方法名: enterpriseBusinessScope
	 * @描述: 经营范围信息确认
	 * @param GMJJHYFL 国民经济行业
	 * @param ZYHY 主营行业
	 * @param JYFW 经营范围
	 * @param MCHZ_BH 业务流水号
	 * @param YWJC_BH 业务进程编号
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2018年12月29日下午4:26:10
	 * @throws
	 */
	@RequestMapping("/jyfwqr")
	@ResponseBody
	public Map<String, Object> enterpriseBusinessScope(@RequestParam("GMJJHYFL") String GMJJHYFL,@RequestParam("ZYHY") String ZYHY,@RequestParam("JYFW") String JYFW,
											  @RequestParam("MCHZ_BH") String MCHZ_BH,@RequestParam("YWJC_BH") String YWJC_BH){
		logger.info("==========enterpriseBusinessScope(String ZYHY,String JYFW,String MCHZ_BH)===========");
		
		if(JYFW.getBytes().length>500) return jsonUtils.serialize(0, "经营范围内容长度超出", 0,"");
		
		validateMchzBh(MCHZ_BH);
		Map<String, Object> data = new HashMap<>();
		//状态
		int status = 0;
		//状态信息
		String msg = "失败！";
		//数据总条数
		int total = 0;
		EnterpriseInf ef = new EnterpriseInf();
		ef.setBh(MCHZ_BH);
		ef.setGmjjhyfl(GMJJHYFL);
		ef.setZyhy(ZYHY);
		ef.setJyfw(JYFW);
		status=enterpriseNameServiceImpl.updateEnterpriseName(ef);
		SegmentProgressInf sf = new SegmentProgressInf();
		//构造业务进程对象，存储当前业务办理的进度相关信息
		sf = getSegmentProgressInfRelated(YWJC_BH, CouldApprovalConstants.YWLX_ENTERPRISENAME, MCHZ_BH, CouldApprovalConstants.YWJZ_BUSINESS_SCOPE, CouldApprovalConstants.YWGX_RELATED_ME, CouldApprovalConstants.CZDZ_BUSINESS_SCOPE, null, null);
		segmentProgressServiceImpl.updateSegmentProgress(sf);
		
		data.put("YWJC_BH", YWJC_BH);//业务进程编号
		data.put("MCHZ_BH", MCHZ_BH);
		msg = "成功！";
		return jsonUtils.serialize(status, msg, total, data);
	}
		
	/**
	 * @方法名: enterpriseAgent
	 * @描述: 指定代表或委托代理人信息确认
	 * @param MCHZ_BH 业务流水号
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2018年12月29日下午5:21:27
	 * @throws
	 */
	@RequestMapping("/dlrxx")
	@ResponseBody
	public Map<String, Object> enterpriseAgent(@RequestParam("MCHZ_BH") String MCHZ_BH){
		logger.info("==========enterpriseAgent(String MCHZ_BH)===========");
		validateMchzBh(MCHZ_BH);//验证业务流水号是否有效
		//状态
		int status = 0;
		//状态信息
		String msg = "失败！";
		//数据总条数
		int total = 0;
		EnterpriseInf ef = new EnterpriseInf();
		//ef.setBh(QYHM_BH);
		Map<String,Object> map = enterpriseNameServiceImpl.selectAgent(MCHZ_BH);
		System.out.println("map:"+map);
		if(map != null) {
			//将代理人信息保存进名称核准表中
			String XM = (String) map.get("XM");
			String SJHM = (String) map.get("SJHM");
			String SFZ_HM = (String) map.get("SFZ_HM");
			ef.setBh(MCHZ_BH);
			ef.setJbr_xm(XM);
			ef.setJbr_zjhm(SFZ_HM);
			ef.setJbr_lxdh(SJHM);
			status=enterpriseNameServiceImpl.updateEnterpriseName(ef);
		}
		msg = "成功！";
		return jsonUtils.serialize(status, msg, total, map);
	}
	
	/**
	 * @方法名: getNameApprovalByContinue
	 * @描述: 根据名称核准编号获取名称核准信息
	 * @param continues 从业务进程点击继续办理进来的，它的值为名称核准编号。为了区分一个委托代理人同时有好几个未办理完成的名称核准业务
	 * @param token 用于用户身份合法性校验
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2018年12月29日下午5:21:27
	 * @throws
	 */
	@RequestMapping("/mchz/hqmchztgid")
	@ResponseBody
	public Map<String, Object> getNameApprovalByContinue(@RequestParam("continues") String continues, @RequestHeader("token") String token){
		logger.info("==========getNameApprovalByContinue(String continues)===========");
		validateMchzBhAndToken(continues, token);
		//状态
		int status = 0;
		//状态信息
		String msg = "失败！";
		//数据总条数
		int total = 0;
		EnterpriseInf ef = enterpriseNameServiceImpl.selectEnterpriseInfByID(continues);
		List<Map<String, Object>> Investor = enterpriseNameServiceImpl.selectInvestors(continues);
		Map<String, Object> data = new HashMap<>();
		data.put("EnterpriseInf", ef);
		data.put("Investor", Investor);
		msg = "成功！";
		if(ef!=null) {
			status = 1;
		}
		return jsonUtils.serialize(status, msg, total, data);
	}
	
	/**
	 * @方法名: pasteIDCard
	 * @描述:	 粘贴身份证
	 * @param MCHZ_BH 业务流水号
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月2日上午10:55:16
	 * @throws
	 */
	@RequestMapping("/ztsfz")
	@ResponseBody
	public Map<String, Object> pasteIDCard(@RequestParam("MCHZ_BH") String MCHZ_BH){
		logger.info("==========pasteIDCard(String MCHZ_BH)===========");
		validateMchzBh(MCHZ_BH);
		//状态
		int status = 0;
		//状态信息
		String msg = "失败！";
		//数据总条数
		int total = 0;
		EnterpriseInf ef = new EnterpriseInf();
		//ef.setBh(QYHM_BH);
		//根据业务流水号获取身份证正反面路径
		Map<String,Object> map = enterpriseNameServiceImpl.selectIDCard(MCHZ_BH);
		if(map != null) {
			String SFZ_ZM = (String) map.get("SFZ_ZM");
			String SFZ_BM = (String) map.get("SFZ_BM");
			ef.setBh(MCHZ_BH);
			ef.setJbr_sfzzb(SFZ_ZM);
			ef.setJbr_sfzbm(SFZ_BM);
			status=enterpriseNameServiceImpl.updateEnterpriseName(ef);
		}
		msg = "成功！";
		return jsonUtils.serialize(status, msg, total, map);
	}
	
	/**
	 * @方法名: agentConfirm
	 * @描述: 代理人确认接口
	 * @param SQQX_KSRQ 授权期限-开始时间（格式2018-11-11）
	 * @param SQQX_JSRQ 授权期限-结束时间（格式2018-12-11）
	 * @param MCHZ_BH 业务流水号
	 * @param SQQX_QSHDYJ 授权权限-签署核对意见（0同意，1不同意）
	 * @param SQQX_XGBG 授权权限-修改表格（0同意，1不同意）
	 * @param SQQX_LQYXHZTZS 授权权限-领取预先核准通知书（0同意，1不同意）
	 * @param YWJC_BH 业务进程编号
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月2日上午11:23:05
	 * @throws
	 */
	@RequestMapping("/dlrqr")
	@ResponseBody
	public Map<String, Object> agentConfirm(@RequestParam("SQQX_KSRQ") String SQQX_KSRQ,@RequestParam("SQQX_JSRQ") String SQQX_JSRQ,
											@RequestParam("MCHZ_BH") String MCHZ_BH,@RequestParam("SQQX_QSHDYJ") String SQQX_QSHDYJ,
											@RequestParam("SQQX_XGBG") String SQQX_XGBG,@RequestParam("SQQX_LQYXHZTZS") String SQQX_LQYXHZTZS,@RequestParam("YWJC_BH") String YWJC_BH){
		logger.info("==========agentConfirm(String SQQX_KSRQ,String SQQX_JSRQ,String MCHZ_BH,String SQQX_QSHDYJ,String SQQX_XGBG,String SQQX_LQYXHZTZS,String YWJC_BH)===========");
		validateMchzBh(MCHZ_BH);
		Map<String, Object> data = new HashMap<>();
		//状态
		int status = 0;
		//状态信息
		String msg = "失败！";
		//数据总条数
		int total = 0;
		EnterpriseInf ef = new EnterpriseInf();
		ef.setBh(MCHZ_BH);
		ef.setSqqx_qshdyj(SQQX_QSHDYJ);
		ef.setSqqx_xgbg(SQQX_XGBG);
		ef.setSqqx_lqyxhztzs(SQQX_LQYXHZTZS);
		ef.setSqqx_ksrq(SQQX_KSRQ);
		ef.setSqqx_jsrq(SQQX_JSRQ);
		status=enterpriseNameServiceImpl.updateEnterpriseName(ef);
		SegmentProgressInf sf = new SegmentProgressInf();
		//构造业务进程对象，存储当前业务办理的进度相关信息
		sf = getSegmentProgressInfRelated(YWJC_BH, CouldApprovalConstants.YWLX_ENTERPRISENAME, MCHZ_BH, CouldApprovalConstants.YWJZ_AGENT, CouldApprovalConstants.YWGX_RELATED_ME, CouldApprovalConstants.CZDZ_AGENT, null, null);
		segmentProgressServiceImpl.updateSegmentProgress(sf);
		
		data.put("YWJC_BH", YWJC_BH);//业务进程编号
		data.put("MCHZ_BH", MCHZ_BH);
		msg = "成功！";
		return jsonUtils.serialize(status, msg, total, data);
	}
	
	/**
	 * @方法名: enterpriseNameConfirm
	 * @描述:  名称核准信息确认
	 * @param MCHZ_BH 业务流水号
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月2日下午2:06:15
	 * @throws
	 */
	@RequestMapping("/mchzqr")
	@ResponseBody
	public Map<String, Object> enterpriseNameConfirm(@RequestParam("MCHZ_BH") String MCHZ_BH){
		logger.info("==========enterpriseNameConfirm(String MCHZ_BH)===========");
		validateMchzBh(MCHZ_BH);
		Map<String, Object> map = new HashMap<>();
		//状态
		int status = 0;
		//状态信息
		String msg = "失败！";
		//数据总条数
		int total = 0;
		EnterpriseInf ef = enterpriseNameServiceImpl.selectEnterpriseInfByID(MCHZ_BH);
		List<Map<String, Object>> list = enterpriseNameServiceImpl.selectInvestors(MCHZ_BH);
		map.put("EnterpriseInf", ef);
		map.put("list", list);
			
		msg = "成功！";
		return jsonUtils.serialize(status, msg, total, map);
	}
		
	/**
	 * @方法名: enterpriseNameNext
	 * @描述:  名称核准业务最终预览页面确认
	 * @param MCHZ_BH 业务流水号
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月19日下午3:53:25
	 * @throws
	 */
	@RequestMapping("/mchzqrxyb")
	@ResponseBody
	public Map<String, Object> enterpriseNameNext(@RequestParam("MCHZ_BH") String MCHZ_BH){
		validateMchzBh(MCHZ_BH);
		//状态
		int status = 0;
		//状态信息
		String msg = "失败！";
		//将所有需要签名的投资人信息添加进业务进程表中
		List<Map<String, Object>> list = enterpriseNameServiceImpl.selectInvestors(MCHZ_BH);
		List<Map<String, Object>> ywjc = new ArrayList<>();
		for(int i =0;i<list.size();i++) {
			Map<String, Object> temp  = new HashMap<>();
			temp.put("ywjc_bh", enterpriseUtil.generateCode());
			temp.put("ywlx", "1");
			temp.put("ywbh", MCHZ_BH);
			temp.put("ywjz", "企业名称核准信息预览");
			temp.put("ywgx", "2");
			temp.put("czdz", "");
			temp.put("wczt", "2");
			temp.put("sfz_xm",list.get(i).get("TZR_XM"));
			temp.put("sfz_hm",list.get(i).get("TZR_ZZHM"));
			ywjc.add(temp);
		}
		int record = segmentProgressServiceImpl.insertSegmentProgressByList(ywjc);
		status = record >0 ? 1 :0;
		EnterpriseInf ef = new EnterpriseInf();
		ef.setBh(MCHZ_BH);
		ef.setBgtxwczt("0");//标识当前所有表格数据已填写完成
		enterpriseNameServiceImpl.updateEnterpriseName(ef);
		
		msg = "成功！";
		return jsonUtils.serialize(status, msg, 0, "");
	}
	
	
	/**
	 * @方法名: deleteInvestor
	 * @描述:  删除指定投资人
	 * @param TZR_BH 投资人编号
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月7日下午2:30:33
	 * @throws
	 */
	@RequestMapping("/sctzr")
	@ResponseBody
	public Map<String, Object> deleteInvestor(@RequestParam("TZR_BH") String TZR_BH){
		logger.info("==========deleteInvestor(String TZR_BH)===========");
		Map<String, Object> map = new HashMap<>();
		//状态
		int status = 0;
		//状态信息
		String msg = "失败！";
		//数据总条数
		int total = 0;
		status=enterpriseNameServiceImpl.deleteInvestor(TZR_BH);
		map.put("TZR_BH", TZR_BH);
		msg = "成功！";
		return jsonUtils.serialize(status, msg, total,map);
	}
		
	/**
	 * @方法名: selectEnterpriseNameInfs
	 * @描述:  返回该用户最近一次办理的核名业务数据
	 * @param token 用于用户身份合法性校验 
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月15日下午4:21:22
	 * @throws
	 */
	@RequestMapping("/mchz/xrym")
	@ResponseBody
	public Map<String, Object> selectEnterpriseNameInfs(@RequestHeader("token") String token){
		String content = desutil.decryptBasedDes(token);
		String[] arr = content.split(",");
		String YHSFZJHM = arr[0];
		Map<String, Object> map = new HashMap<>();
		List<EnterpriseInf> list = enterpriseNameServiceImpl.selectEnterpriseNameInfs(YHSFZJHM);
		//用登记日期对list中的对象进行降序排序
		Collections.sort(list, new Comparator<EnterpriseInf>(){
			@Override
			public int compare(EnterpriseInf o1, EnterpriseInf o2) {
				Date d1 = o1.getDjrq();
				Date d2 = o2.getDjrq();
				if(d1.before(d2)) {
					return 1;
				}else if(d1.equals(d2)) {
					return 0;
				}
				return -1;
			}
		});
		//返回该名称核准业务数据以及相关的所有投资人信息
		if(list.size() > 0) {
			String MCHZ_BH = list.get(0).getBh();
			List<Map<String, Object>> Investor = enterpriseNameServiceImpl.selectInvestors(MCHZ_BH);
			Map<String, Object> param = segmentProgressServiceImpl.selectYWJCBH(MCHZ_BH, YHSFZJHM);
			map.put("EnterpriseInf", list.get(0));
			map.put("Investor", Investor);
			map.put("param", param);
			return jsonUtils.serialize(1, "", 0,map);
		}else {
			return jsonUtils.serialize(1, "", 0,"");
		}
	}
	
	
	/**
	 * @方法名: selectSegmentProgressInf
	 * @描述:  加载当前用户未完成的业务信息
	 * @param token 用于用户身份合法性校验 
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月19日下午4:08:09
	 * @throws
	 */
	@RequestMapping("/mchz/ywjc")
	@ResponseBody
	public Map<String, Object> selectSegmentProgressInf(@RequestHeader("token") String token){
		String content = desutil.decryptBasedDes(token);
		String[] arr = content.split(",");
		String YHSFZJHM = arr[0];
		List<Map<String,Object>> list = segmentProgressServiceImpl.selectYWJCInf(YHSFZJHM);
		if(list.size()>0) {
			return jsonUtils.serialize(1, "", 0,list);
		}else {
			return jsonUtils.serialize(1, "", 0,"");
		}
	}
	
	
	/**
	 * @方法名: selectSegmentProgressSignatureInf
	 * @描述:  需我签名
	 * @param token 用于用户身份合法性校验 
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月21日上午9:52:55
	 * @throws
	 */
	@RequestMapping("/mchz/ywjcqm")
	@ResponseBody
	public Map<String, Object> selectSegmentProgressSignatureInf(@RequestHeader("token") String token){
		//String idCardNum = authenticationServiceImpl.selectInvestorIdNum(token);
		String content = desutil.decryptBasedDes(token);
		String[] arr = content.split(",");
		String YHSFZJHM = arr[0];
		List<Map<String,Object>> list = segmentProgressServiceImpl.selectYWJCSignatureInf(YHSFZJHM);
		if(list.size()>0) {
			return jsonUtils.serialize(1, "", 0,list);
		}else {
			return jsonUtils.serialize(1, "", 0,"");
		}
	}
	/**
	 * @方法名: selectSegmentProgressSignatureInf
	 * @描述:  业务进程签名阶段的企业各项信息
	 * @param MCHZ_BH 业务流水号
	 * @param YWJC_BH 业务进程编号
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月21日上午9:52:55
	 * @throws
	 */
	@RequestMapping("/ywjcqyxx")
	@ResponseBody
	public Map<String, Object> selectEnterpriseInf(@RequestParam("MCHZ_BH") String MCHZ_BH,@RequestParam(value="YWJC_BH",required=false) String YWJC_BH){
		validateMchzBh(MCHZ_BH);
		int status = 0;
		Map<String, Object> map = new HashMap<>();
		EnterpriseInf enterpriseInf = new EnterpriseInf();
		enterpriseInf = enterpriseNameServiceImpl.selectEnterpriseInfByID(MCHZ_BH);
		List<Map<String, Object>> investor = enterpriseNameServiceImpl.selectInvestors(MCHZ_BH);
		if(enterpriseInf != null&&investor.size()>0) {
			status = 1;
		}
		map.put("enterpriseInf", enterpriseInf);
		map.put("investors", investor);
				
		return jsonUtils.serialize(status, "加载进程信息有误！", 0,map);
	}
	
	/**
	 * @方法名: selectSegmentProgressSignatureInf
	 * @描述:  加载当前用户身份证正反面url
	 * @param token 用于用户身份合法性校验 
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月21日上午9:52:55
	 * @throws
	 */
	@RequestMapping("/mchz/tzrsfz")
	@ResponseBody
	public Map<String, Object> selectEnterpriseInf(@RequestHeader("token") String token){
		int status = 0;
		Map<String, Object> map = authenticationServiceImpl.selectIdCardUrl(token);
		if(map != null) {
			status = 1;
		}		
		return jsonUtils.serialize(status, "加载进程信息有误！", 0,map);
	}
	
	/**
	 * @方法名: signatureConfirm
	 * @描述:  投资人签名确认提交
	 * @param request
	 * @param token 用于用户身份合法性校验 
	 * @param MCHZ_BH 业务流水号
	 * @param signature 客户端提交过来的签名图片文件（base64格式）
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月23日上午10:08:17
	 * @throws
	 */
	@RequestMapping("/qmqr")
	@ResponseBody
	public Map<String, Object> signatureConfirm(HttpServletRequest request,String token,String MCHZ_BH,String signature){
		validateMchzBhAndToken(MCHZ_BH, token);
		//保存签名照片
		String idCardNum = authenticationServiceImpl.selectInvestorIdNum(token);
		String path=PropertyUtil.loadProps("config/fileconfig.properties", "qmimg");
		String fileName = path+idCardNum+"_qm.png";
		//去掉base64签名文件的文件头，只保留文件体以便解析
		String data = signature.substring(signature.indexOf("base64,")+7);
		byte[] encrypted1 = null;
		try {
			encrypted1 = new BASE64Decoder().decodeBuffer(data);
			File dir = new File(path);
			if (!dir.exists() && !dir.isDirectory()) {
				dir.mkdir();
			}
			//构造文件对象，以字节文件输出流的方式将签名图片写进该对象内
			File file = new File(fileName);
			FileOutputStream out = new FileOutputStream(file);
			out.write(encrypted1);
			out.flush();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		int status = 0;
		//更新当前投资人的签名状态等信息
		//String path = savaSignature(signature, request, token);
		InvestorInf investorInf = new InvestorInf();
		investorInf.setBh(MCHZ_BH);
		investorInf.setTzr_qzhgz("/signature/"+idCardNum+"_qm.png");
		investorInf.setTzr_zzhm(idCardNum);
		investorInf.setTzr_qzhgzrq(new Date());
		investorInf.setTzr_qzhgzzt("0");//将投资人签字盖章状态改为0，代表已签名
		status = investorInfServiceImpl.updateInvestorSignaturePath(investorInf);
		
		//查询当前业务所有投资人
		List<Map<String,Object>> list = investorInfServiceImpl.selectSignatureStatus(MCHZ_BH);
		//判断是否所有投资人已完成签名
		boolean flag = true;
		for(int i=0;i<list.size();i++) {			
			if("1".equals(list.get(i).get("TZR_QZHGZZT"))) {
				flag = false;
				}
			}
		if(flag) {//如果所有投资人签名已完成
			EnterpriseInf ef = new EnterpriseInf();
			ef.setBh(MCHZ_BH);
			ef.setWctbzt("0");			
			segmentProgressServiceImpl.deleteYWJC(MCHZ_BH);
			EnterpriseInf enterpriseInf = enterpriseNameServiceImpl.selectEnterpriseInfByID(MCHZ_BH);
			List<Map<String,Object>> investors = enterpriseNameServiceImpl.selectInvestors(MCHZ_BH);
			//构造word模板对象
			WordTemplate word = new WordTemplate();
			//读取配置文件获取word模板文档路径
			String wroddocin=PropertyUtil.loadProps("config/fileconfig.properties", "wroddocin");
			//读取配置文件获取word输出文档路径
			String wroddocout=PropertyUtil.loadProps("config/fileconfig.properties", "wroddocout");
			//根据word模板生成名称核准word文件
			String templatePath = word.generateWrodTemplate(wroddocin, wroddocout, investors, enterpriseInf);
			//读取配置文件获取pdf文件输出路径,将生成的word文件转换为pdf
			String pdfpath=PropertyUtil.loadProps("config/fileconfig.properties", "generatepdf");
			word.doc2pdf(templatePath, pdfpath+"企业名称预先核准申请书_输出.pdf");
			//使用虚拟路径映射pdf生成路径
			ef.setXtscwj("/pdfdoc/企业名称预先核准申请书_输出.pdf");
			int record = enterpriseNameServiceImpl.updateEnterpriseName(ef);
			status = record == 1?status:0;
		}		
		return jsonUtils.serialize(status, "！", 0,"");
		
	}
	
	/**
	 * @方法名: getSegmentProgressInfRelated
	 * @描述:  构造与我相关类型的对象
	 * @param id 流水号（记录流水号，非业务流水号）
	 * @param ywlx 业务类型(1名称预先核准、2企业设立、3企业变更、4企业注销、5企业备案、6经营许可)
	 * @param ywbh 业务类型所对应的业务流水编号（归属业务类型表的业务编号）
	 * @param ywjz 业务进程标识（如：完善公司地址）
	 * @param ywgx 业务关系（1与我有关、2需我签字）
	 * @param czdz 业务跳转操作URL地址
	 * @param zcyh_bh 归属注册用户编号(业务关系为需我签字时可能为空，目标用户未注册)
	 * @return
	 * @返回类型 SegmentProgressInf
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月18日下午4:45:45
	 * @throws
	 */
	private SegmentProgressInf getSegmentProgressInfRelated(String id,String ywlx,String ywbh,String ywjz,String ywgx,String czdz,String yhsfzjhm,String wczt) {
		SegmentProgressInf sf = new SegmentProgressInf();
		sf.setBh(id);
		sf.setYwlx(ywlx);
		sf.setYwbh(ywbh);
		sf.setYwjz(ywjz);
		sf.setYwgx(ywgx);
		sf.setCzdz(czdz);
		if(wczt != null) {
		sf.setWczt(wczt);
		}
		if(yhsfzjhm != null) {
		sf.setYhsfzjhm(yhsfzjhm);
		}
		return sf;
	}
	
	/**
	 * @方法名: savaImg
	 * @描述:  上传图片
	 * @param file 签名图片对象
	 * @param request
	 * @param BH
	 * @param flag
	 * @return
	 * @返回类型 String
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月15日下午1:53:03
	 * @throws
	 */
	private String savaSignature(CommonsMultipartFile file,HttpServletRequest request,String BH) {
		String fileName =file.getOriginalFilename();
		//读取配置文件获取上传签名路径
		String path=PropertyUtil.loadProps("config/fileconfig.properties", "qmimg");
		String extensionname = fileName.substring(fileName.lastIndexOf(".") + 1);
		String newFilename = BH + "." + extensionname;
		String savePath=path+newFilename;
		try {
			file.transferTo(new File(savePath));
			//File tarFile=new File(path,newFilename);
			//WaterMarkGenerate.generateWithTextMark(tarFile, path+"logo_"+newFilename, "与原件一致！");
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
		return "/signature/"+newFilename;
	}
	
	/**
	 * @方法名: selectIdCardAndSignPicture
	 * @描述:  查询当前用户身份证正反照片与签名照片
	 * @param mchzBh 名称核准编号
	 * @param token 用户信息token
	 * @创建人 djw
	 * @创建时间 2019年2月14日下午3:17:32
	 * @return
	 */
	@RequestMapping("/mchz/sfzqmzp")
	@ResponseBody
	public Map<String, Object> selectIdCardAndSignPicture(@RequestParam("mchzBh") String mchzBh, @RequestHeader("token") String token){
		logger.info("==========selectIdCardAndSignPicture===========");
		validateMchzBhAndToken(mchzBh, token);
		Map<String, Object> map = new HashMap<>();
		//状态
		int status = 0;
		//状态信息
		String msg = "失败！";
		//数据总条数
		int total = 0;
		List<Map<String,Object>> listPhotoUrl = enterpriseNameServiceImpl.selectIdCardAndSignPicture(mchzBh, token);
		if(listPhotoUrl.size()>0) {
			map.put("list", listPhotoUrl);
			status = 1;
		}else {
			map.put("list", "");
		}
		msg = "成功！";
		return jsonUtils.serialize(status, msg, total, map);
	}
	
	
	/**
	 * @方法名: selectIdCardAndSignPicture
	 * @描述:  返回国民经济行业
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 Administrator
	 * @创建时间 2019年2月26日上午10:44:11
	 * @throws
	 */
	@RequestMapping("/gmjjhy")
	@ResponseBody
	public Map<String, Object> selectGmjjhy(){
		//状态
		int status = 0;
		//状态信息
		String msg = "失败！";
		//数据总条数
		int total = 0;
		Map<String, Object> map = new HashMap<>();
		List<Map<String,Object>> gmjjhy = enterpriseNameServiceImpl.selectGmjjhy();
		map.put("gmjjhy", gmjjhy);
		if(gmjjhy.size()>0) {
			//查询第一个国民经济行业对应的主营行业
			List<Map<String,Object>> zyhy = enterpriseNameServiceImpl.selectZyhy(gmjjhy.get(0).get("LBDM").toString());
			map.put("zyhy", zyhy);
			status = 1;
			msg = "成功！";
		}
		return jsonUtils.serialize(status, msg, total, map);
	}
	
	/**
	 * @方法名: selectZyhy
	 * @描述:  主营行业
	 * @param LBDM 行业类别代码
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 Administrator
	 * @创建时间 2019年2月26日下午4:19:38
	 * @throws
	 */
	@RequestMapping("/zyhy")
	@ResponseBody
	public Map<String, Object> selectZyhy(@RequestParam("LBDM") String LBDM){
		//状态
		int status = 0;
		//状态信息
		String msg = "失败！";
		//数据总条数
		int total = 0;
		//查询该行业类别下的所有子类
		List<Map<String,Object>> gmjjhy = enterpriseNameServiceImpl.selectZyhy(LBDM);
		if(gmjjhy.size()>0) {
			status = 1;
			msg = "成功！";
		}
		return jsonUtils.serialize(status, msg, total, gmjjhy);
	}
	
	/**
	 * @方法名: selectZyhy
	 * @描述:  辅助选择
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 Administrator
	 * @创建时间 2019年2月26日上午10:53:10
	 * @throws
	 */
	@RequestMapping("/fzxz")
	@ResponseBody
	public Map<String, Object> selectFuzhu(@RequestParam("LBDM") String LBDM){
		//状态
		int status = 0;
		//状态信息
		String msg = "失败！";
		//数据总条数
		int total = 0;
		List fuzhu = new ArrayList();
		//查询当前行业类别底下的所有子类
		List<Map<String,Object>> gmjjhy = enterpriseNameServiceImpl.selectZyhy(LBDM);
		if(gmjjhy.size()>0) {
			status = 1;
			msg = "成功！";
		for(int i=0;i<gmjjhy.size();i++) {
			String a = gmjjhy.get(i).get("LBDM").toString();
			//查询当前行业类别底下的所有子类的子类
			List<Map<String,Object>> list = enterpriseNameServiceImpl.selectZyhy(gmjjhy.get(i).get("LBDM").toString());
			list.add(0,gmjjhy.get(i));
			fuzhu.add(list);
		}		
	}
		if(gmjjhy.size()>0) {
			status = 1;
			msg = "成功！";
		}		
		return jsonUtils.serialize(status, msg, total, fuzhu);
	}
	
	/**
	 * @方法名: validateMchzBhAndToken
	 * @描述:  验证业务流水号以及对应的token
	 * @param mchzBh
	 * @param token
	 * @返回类型 void
	 * @创建人 djw
	 * @创建时间 2019年2月14日下午3:17:32
	 * @throws
	 */
	private void validateMchzBhAndToken(String mchzBh, String token) {
		EnterpriseInf ef = enterpriseNameServiceImpl.selectEnterpriseInfByID(mchzBh);
		if(ef==null) throw new RuntimeException("名称核准编号不存在");
		String content = desutil.decryptBasedDes(token);
		String[] arr = content.split(",");
		String sfzHm = arr[0];
		InvestorInf nf = enterpriseNameServiceImpl.selectInvestorsByMchzBhAndSfzhm(mchzBh, sfzHm);
		if((!sfzHm.equals(ef.getYhsfzjhm())) && nf==null) throw new RuntimeException("身份证号码有误");
	}
		
	/**
	 * @方法名: validateMchzBh
	 * @描述:  验证业务流水号
	 * @param mchzBh
	 * @返回类型 void
	 * @创建人 djw
	 * @创建时间 2019年2月14日下午3:18:06
	 * @throws
	 */
	private void validateMchzBh(String mchzBh) {
		EnterpriseInf ef = enterpriseNameServiceImpl.selectEnterpriseInfByID(mchzBh);
		if(ef==null) throw new RuntimeException("名称核准编号不存在");
	}
	
	public static void main(String[] args) {
		DES_Utils  desutil = new DES_Utils();
		String content = desutil.decryptBasedDes("ATEgS7zC1RMm5nKKpoQRALrQkGiktcsrgBF80YpjHPkjOT6GvrRKXQ==");
		String[] arr = content.split(",");
		String YHSFZJHM = arr[0];
		System.out.println(YHSFZJHM);
	}
	
	/**
	 * @方法名: selectIdCardAndSignPicture
	 * @描述:  客户端调用行业参考经营范围
	 * @param mchzBh 名称核准编号
	 * @param token 用户信息token
	 * @创建人 djw
	 * @创建时间 2019年2月23日下午16:48:02
	 * @return  
	 */
	@RequestMapping("/jyfwJbxx")
	@ResponseBody
	public Map<String, Object> selectJyfwJbxxByMchzBhAndToken(@RequestParam("mchzBh") String mchzBh, @RequestHeader("token") String token){
		logger.info("==========selectJyfwJbxxByMchzBhAndToken===========");
		validateMchzBhAndToken(mchzBh, token);
		Map<String, Object> data = new HashMap<>();
		//状态
		int status = 0;
		//状态信息
		String msg = "失败！";
		//数据总条数
		int total = 0;
		EnterpriseInf ef = enterpriseNameServiceImpl.selectEnterpriseInfByID(mchzBh);
		List<Map<String, Object>> sx = sczrJyfwJbxxService.selectJyfwJbxxByMchzBhAndToken(ef.getHytd());
		if(sx.size()==0) return jsonUtils.serialize(0, "加载信息失败！", 0,"");
		data.put("jyfwJbxx", sx);
		msg = "成功！"; 
		return jsonUtils.serialize(status, msg, total, data);
	}
	
	
}
