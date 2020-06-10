package com.kx.diesel.check.controller;

import com.kx.basic.util.HkJson;
import com.kx.diesel.check.service.SerialPortService;
import com.kx.diesel.check.util.SerialTool;
import gnu.io.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author Liumin
 * @date 2020-02-14 13:49
 */
@RestController
@RequestMapping("/serialPortController")
public class SerialPortController {

	private static Logger logger = LoggerFactory.getLogger(SerialPortController.class);

	@Autowired
	private SerialPortService serialPortService;


	/**
	 * @Description: 获取串口集合
	 * @Author: liumin
	 * @Date: 2020-02-14
	 */
	@PostMapping(value = "/getSerialPortList")
	public HkJson getSerialPortList(@RequestBody Map<String,Object> params) {
		HkJson hkJson = new HkJson();
		try {
			List<String> serialPortList= SerialTool.getSerialPortList();
			hkJson.setData(serialPortList).success("获取系统串口集合成功！");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return hkJson;
	}

	/**
	 * @Description: 检测设备
	 * @Author: liumin
	 * @Date: 2020-02-14
	 * @param: checks:true(检测仪器) false(根据频率获取数据)
	 */
	@PostMapping(value = "/getRealTimeTest")
	public HkJson getRealTimeTest(@RequestBody Map<String,Object> params) {
		HkJson hkJson = new HkJson();
		List<String> smokeList = new ArrayList();
		// 检测 获取数据
		boolean checks = (boolean) params.get("checks");
		/*// 端口名称
		String smokeSerialPortName = (String) params.get("smokeSerialPortName");
		// 波特率
		String smokeBaudRate = (String) params.get("smokeBaudRate");
		// 烟度计获取频率
		String smokeFrequency = (String) params.get("smokeFrequency");*/
		String checkId = (String) params.get("checkId");
		if(StringUtils.isBlank(checkId)){
			hkJson.fail("检测设备为空！");
			return hkJson;
		}

		if(StringUtils.isBlank((String) params.get("smokeSerialPortName")) || StringUtils.isBlank((String) params.get("smokeBaudRate"))){
			hkJson.fail("请准确输入通讯端口和波特率值！");
			return hkJson;
		}
		//serialPortService.getRealTimeTest(checks,smokeSerialPortName,smokeBaudRate,smokeFrequency);
		hkJson = serialPortService.getRealTimeTest(params);
		return hkJson;
	}

	/**
	 * @Description: 开始检测 自由加速试验
	 * @Author: liumin
	 * @Date: 2020-02-14
	 */
	@PostMapping(value = "/getFreeAcceleration")
	public HkJson getFreeAcceleration(@RequestBody Map<String,Object> params)
		throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException {
		HkJson hkJson = new HkJson();
		String checkId = (String) params.get("checkId");

		if(StringUtils.isBlank(checkId)){
			hkJson.fail("检测设备为空！");
			return hkJson;
		}
		hkJson = serialPortService.getFreeAcceleration(params);
		return hkJson;
	}

	/**
	 * @Description: 取烟度计测量的所有数据
	 * @Author: liumin
	 * @Date: 2020-02-14
	 * @param
	 */
	@PostMapping(value = "/getAllData")
	public HkJson getAllData(@RequestBody Map<String,Object> params) {
		HkJson hkJson = new HkJson();
		// 端口名称
		String smokeSerialPortName = (String) params.get("smokeSerialPortName");
		// 波特率
		String smokeBaudRate = (String) params.get("smokeBaudRate");
		// 烟度计获取频率
		String smokeFrequency = (String) params.get("smokeFrequency");

		if(StringUtils.isBlank(smokeSerialPortName) || StringUtils.isBlank(smokeBaudRate)){
			hkJson.fail("请准确输入通讯端口和波特率值！");
			return hkJson;
		}
		hkJson = serialPortService.getAllData(params);
		return hkJson;
	}

	/**
	 * @Description: 获取自由加速试验状态
	 * @Author: liumin
	 * @Date: 2020-02-14
	 * @param
	 */
	@PostMapping(value = "/getFreeAccelerationStatus")
	public HkJson getFreeAccelerationStatus(@RequestBody Map<String,Object> params) {
		HkJson hkJson = new HkJson();
		List<String> smokeList = new ArrayList();
		// 端口名称
		String smokeSerialPortName = (String) params.get("smokeSerialPortName");
		// 波特率
		String smokeBaudRate = (String) params.get("smokeBaudRate");
		if(StringUtils.isBlank(smokeSerialPortName) || StringUtils.isBlank(smokeBaudRate)){
			hkJson.fail("请准确输入通讯端口和波特率值！");
			return hkJson;
		}
		hkJson = serialPortService.getFreeAccelerationStatus(params);
		return hkJson;
	}


}
