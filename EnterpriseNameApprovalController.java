
/**
 * @标题: EnterpriseNameApprovalController.java
 * @包名： com.saar.gov.controller
 * @功能描述：TODO
 * @作者： dongchenhui
 * @创建时间： 2019年1月10日 下午3:05:50
 * @version v1.0
 */

package com.saar.gov.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.saar.gov.beans.EnterpriseInf;
import com.saar.gov.beans.InvestorInf;
import com.saar.gov.beans.StudentInf;
import com.saar.gov.beans.WwxtZcyh;
import com.saar.gov.service.api.AuthenticationService;
import com.saar.gov.service.api.EnterpriseNameService;
import com.saar.gov.service.api.InvestorInfService;
import com.saar.gov.service.api.SegmentProgressService;
import com.saar.gov.service.impl.EnterpriseNameServiceImpl;
import com.saar.gov.util.ApprovalJsonUtils;
import com.saar.gov.util.DES_Utils;
import com.saar.gov.util.JsonUtils;
import com.saar.gov.util.WeChatPostMsgUtil;

/**
 * @项目名称：gov
 * @包名： com.saar.gov.controller
 * @类名称：EnterpriseNameApprovalController
 * @类描述： 企业名称核准审批
 * @创建人：dongchenhui
 * @创建时间：2019年1月10日下午3:05:50
 * @修改人：dongchenhui
 * @修改时间：2019年1月10日下午3:05:50
 * @修改备注：
 * @version v1.0
 */
@Controller
@RequestMapping("/samr/mzhzsp")
public class EnterpriseNameApprovalController {
	private static final Logger logger = LoggerFactory.getLogger(EnterpriseNameApprovalController.class);
	
	@Autowired
	ApprovalJsonUtils approvalJsonUtils;
	
	@Autowired
	EnterpriseNameService enterpriseNameServiceImpl;
	
	@Autowired
	InvestorInfService investorInfServiceImpl;
	
	@Autowired
	AuthenticationService authenticationServiceImpl;
	
	@Autowired
	SegmentProgressService SegmentProgressServiceImpl;
	
	@Autowired
	DES_Utils desutil;// DES加密工具
	
	
	/**
	 * @方法名: selectEnterpriseNameInfByStatus
	 * @描述: 根据审批状态查询企业核名信息
	 * @param SHZT 审核状态
	 * @return
	 * @返回类型 List<EnterpriseInf>
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月10日下午3:55:31
	 * @throws
	 */
	@RequestMapping("/cxzt")
	@ResponseBody
	public Map<String, Object> selectEnterpriseNameInfByStatus(@RequestParam("SHZT") String SHZT, @RequestParam(value = "qymc",required=false)String qymc,@RequestParam(value = "QYLX",required=false)String QYLX,@RequestParam(value = "QYZS_XIAN",required=false)String QYZS_XIAN){
		logger.info("==========selectEnterpriseNameInfByStatus(String SHZT)===========");
		//状态
		int code = 1;
		//状态信息
		String msg = "未查询到数据！";
		//数据总条数
		int total = 0;
		List<EnterpriseInf> list = enterpriseNameServiceImpl.selectEnterpriseNameInfByStatus(SHZT, qymc, QYLX, QYZS_XIAN);
		msg = list.size()>0?"成功！":msg;
		code = list.size()>0?0:code;
		return approvalJsonUtils.serialize(code, msg, list.size(), list);
	}
		
	/**
	 * @方法名: enterpriseNameConfirm
	 * @描述: 查询单个核准业务信息详情
	 * @param MCHZ_BH 业务流水号
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月10日下午4:40:05
	 * @throws
	 */
	@RequestMapping("/cxdgxx")
	@ResponseBody
	public Map<String, Object> selectEnterpriseNameInfById(@RequestParam("MCHZ_BH") String MCHZ_BH){
		logger.info("==========selectEnterpriseNameInfById(String MCHZ_BH)===========");
		//状态
		int status = 1;
		//状态信息
		String msg = "失败！";
		//数据总条数
		int total = 0;
		EnterpriseInf ef = enterpriseNameServiceImpl.selectEnterpriseInfByID(MCHZ_BH);
		msg = "成功！";
		status = 0;
		return approvalJsonUtils.serialize(status, msg, total, ef);
	}
		
	/** 
	 * @方法名: selectEnterpriseNameInfChecked
	 * @描述:  查询所有企业核名信息接口
	 * @param MCHZ_BH 业务流水号
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月12日下午6:07:57
	 * @throws
	 */
	@RequestMapping("/cxsyqy")   
	@ResponseBody
	public Map<String, Object> selectEnterpriseNameInfChecked(@RequestParam("MCHZ_BH") String MCHZ_BH){
		logger.info("==========selectEnterpriseNameInfById(String MCHZ_BH)===========");
		//状态
		int status = 1;
		//状态信息
		String msg = "失败！";
		//数据总条数
		int total = 0;
		List<EnterpriseInf> list = enterpriseNameServiceImpl.selectEnterpriseNameInf();
		msg = "成功！";
		status = 0;
		return approvalJsonUtils.serialize(status, msg, total, list);
	}
	
	/**
	 * @方法名: updateSHZT
	 * @描述:  审核状态变更
	 * @param MCHZ_BH 业务流水号
	 * @param SHJG 审核结果
	 * @param JBR_ZJHM 经办人证件号码
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月14日下午2:04:41
	 * @throws
	 */
	@RequestMapping("/shztbg")
	@ResponseBody
	public Map<String, Object> updateSHZT(@RequestParam("MCHZ_BH") String MCHZ_BH,@RequestParam("SHJG") String SHJG,@RequestParam("JBR_ZJHM") String JBR_ZJHM){
		logger.info("==========selectEnterpriseNameInfById(String MCHZ_BH)===========");
		EnterpriseInf ef = new EnterpriseInf();
		ef.setBh(MCHZ_BH);
		ef.setShzt("1");//设为已审核
		ef.setShjg(SHJG);
		//状态
		int status = 0;
		//状态信息
		String msg = "失败！";
		//数据总条数
		int total = 0;
		enterpriseNameServiceImpl.updateEnterpriseName(ef);
		
		String openid = authenticationServiceImpl.selectOpenId(JBR_ZJHM);
		WeChatPostMsgUtil wl = new WeChatPostMsgUtil();
		WwxtZcyh wh = new WwxtZcyh();
		wl.postMsg(wh, openid, "wx19f1c055104760ff", "486e0adb0101337589285cffdab928d9");
				
		msg = "成功！";
		status = 1;
		return approvalJsonUtils.serialize(status, msg, total, status);
	}
	
	
	/**
	 * @方法名: refused
	 * @描述:  审核不通过
	 * @param MCHZ_BH 业务流水号 
	 * @param reason 审核不通过的理由
	 * @return
	 * @返回类型 Map<String,Object>
	 * @创建人 dongchenhui
	 * @创建时间 2019年1月31日上午10:16:46
	 * @throws
	 */
	@RequestMapping("/shbtg")
	@ResponseBody
	public Map<String, Object> refused(@RequestParam("MCHZ_BH") String MCHZ_BH,@RequestParam("reason") String reason){
		int status = 0;
		//状态信息
		String msg = "失败！";
		//数据总条数
		int total = 0;
		EnterpriseInf ef = new EnterpriseInf();
		ef.setBh(MCHZ_BH);
		ef.setShzt("0");//设为已审核
		ef.setShjg("");
		ef.setBgtxwczt("1");
		ef.setWctbzt("1");
		status = enterpriseNameServiceImpl.updateEnterpriseName(ef);
		
		InvestorInf iv = new InvestorInf();
		iv.setMchz_bh(MCHZ_BH);
		iv.setTzr_qzhgzzt("1");
		iv.setTzr_qzhgz("");
		investorInfServiceImpl.updateInvestorStatus(iv);
		SegmentProgressServiceImpl.deleteInvestors(MCHZ_BH);
		SegmentProgressServiceImpl.updateYWJCWCZT(MCHZ_BH);
		String openid = enterpriseNameServiceImpl.selectOpenID(MCHZ_BH);
		WeChatPostMsgUtil wl = new WeChatPostMsgUtil();
		wl.postDenyMsg(reason, openid, "wx19f1c055104760ff", "486e0adb0101337589285cffdab928d9");
		return approvalJsonUtils.serialize(status, msg, total, status);
		
	}
	
	/**
	 * 模糊查询企业信息
	 * @return
	 */
	@RequestMapping("/mhcxqy")
	@ResponseBody
	public Map<String, Object> fuzzySelect(@RequestParam(value = "qymc",required=false)String qymc,@RequestParam(value = "QYLX",required=false)String QYLX,@RequestParam(value = "QYZS_XIAN",required=false)String QYZS_XIAN) {
		logger.info("==========fuzzySelect===========");
		int status = 1;
		String mString="失败！";
		int total = 0;
		List<EnterpriseInf> list = enterpriseNameServiceImpl.fuzzySelect(qymc, QYLX, QYZS_XIAN);
		mString = "成功!";
		status = 0;
		total = list.size();
		return approvalJsonUtils.serialize(status, mString, total, list);
		
	}
	
	@RequestMapping("/mchz/shbtg")
	@ResponseBody
	public Map<String, Object> selectIdCardAndSignPicture(@RequestParam("reason") String reason,@RequestParam("token") String token){
		//状态
		int status = 0;
		//状态信息
		String msg = "失败！";
		//解析token，获取用户身份证号码
		String content = desutil.decryptBasedDes(token);
		String[] arr = content.split(",");
		String YHSFZJHM = arr[0];
		String openid = authenticationServiceImpl.selectOpenId(YHSFZJHM);
		WeChatPostMsgUtil wl = new WeChatPostMsgUtil();
		wl.postDenyMsg(reason, openid, "wx19f1c055104760ff", "486e0adb0101337589285cffdab928d9");
		msg = "成功！";
		status = 1;
		return approvalJsonUtils.serialize(status, msg, 0, status);
	}
	
	@RequestMapping("/test")
	@ResponseBody
	public Map<String, Object> test(@RequestBody StudentInf stu){
		
		
		String b = stu.toString();
		
		return approvalJsonUtils.serialize(1, "", 0, "");
	}
	
	
	
	
	
	
	
}
