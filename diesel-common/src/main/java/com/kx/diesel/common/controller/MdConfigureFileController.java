package com.kx.diesel.common.controller;

import com.kx.basic.controller.BasicController;
import com.kx.basic.util.HkJson;
import com.kx.diesel.common.entity.MdConfigureFile;
import com.kx.diesel.common.service.MdConfigureFileService;
import com.kx.security.utils.BuildDefault;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/mdConfigureFileController")
public class MdConfigureFileController extends BasicController<MdConfigureFile>{

	private static Logger logger = LoggerFactory.getLogger(MdConfigureFileController.class);

	@Autowired
	private MdConfigureFileService mdConfigureFileService;

	@Autowired
	private BuildDefault<MdConfigureFile> fileBuildDefault;

	@Override
	public MdConfigureFileService getService(){
		return this.mdConfigureFileService;
	}

	/*
	 * @Author skk
	 * @Description //TODO admin 查询文件列表
	 * @Date 2020/3/27
	 **/
	@PostMapping(value = "/listFile")
	public HkJson listFile(@RequestBody Map<String, Object> params) {
		HkJson hkJson = new HkJson();
		params = super.initPage(params);
		hkJson = this.getService().listFile(params);
		return hkJson;
	}

	/*
	 * @Author skk
	 * @Description //TODO admin 上传文件
	 * @Date 2020/3/27
	 **/
	@PostMapping(value = "/addFile")
	public HkJson addFile(@RequestBody MdConfigureFile file){
		HkJson hkJson = new HkJson();
		if (null == file || StringUtils.isBlank(file.getId())) {
			return hkJson.fail("缺少参数");
		}
		try {
			fileBuildDefault.buildSaveColumn(file);
			this.getService().insert(file);
			hkJson.success();
		} catch (Exception e) {
			e.printStackTrace();
			hkJson.fail("上传失败");
		}
		return hkJson;
	}

	/*
	 * @Author skk
	 * @Description //TODO admin 删除文件
	 * @Date 2020/3/27
	 **/
	@PostMapping(value = "/delFile")
	public HkJson delFile(@RequestBody MdConfigureFile file) {
		HkJson hkJson = new HkJson();
		if (null == file || StringUtils.isBlank(file.getId())) {
			return hkJson.fail("缺少参数");
		}
		try {
			this.getService().delete(file);
			hkJson.success();
		} catch (Exception e) {
			e.printStackTrace();
			hkJson.fail("删除失败");
		}
		return hkJson;
	}

	/*
	 * @Author skk
	 * @Description //TODO admin 查询文件列表 转base64
	 * @Date 2020/3/27
	 **/
	@PostMapping(value = "/listFileByBase")
	public HkJson listFileByBase(@RequestBody Map<String, Object> params) {
		HkJson hkJson = new HkJson();
		params = super.initPage(params);
		hkJson = this.getService().listFileByBase(params);
		return hkJson;
	}


}