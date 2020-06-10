package com.kx.diesel.common.controller;

import com.kx.basic.controller.BasicController;
import com.kx.basic.util.HkJson;
import com.kx.basic.util.UuidUtils;
import com.kx.diesel.common.entity.HkCommonStudent;
import com.kx.diesel.common.service.HkCommonStudentService;
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
@RequestMapping("/hkCommonStudentController")
public class HkCommonStudentController extends BasicController<HkCommonStudent>{

	private static Logger logger = LoggerFactory.getLogger(HkCommonStudentController.class);

	@Autowired
	private HkCommonStudentService hkCommonStudentService;

	@Override
	public HkCommonStudentService getService(){
		return this.hkCommonStudentService;
	}

    /**
     * @Author zxq
     * @Description //查询所有学生
     * @Date 2020/05/20
     */

    @PostMapping(value = "/listStudent")
    public HkJson listStudent(@RequestBody Map<String, Object> params) {
        HkJson hkJson = new HkJson();
        params = super.initPage(params);
        if (params == null) {
            hkJson.fail("传入参数为空");
            return hkJson;
        }
        return this.getService().listStudent(params);
    }

    /*
     * @Author zxq
     * @Description //TODO admin 删除学生
     * @Date 2020/5/20
     **/
    @PostMapping(value = "/delStudent")
    public HkJson delCategory(@RequestBody HkCommonStudent student) {
        HkJson hkJson = new HkJson();
        if (null == student|| StringUtils.isBlank(student.getId()))
            return hkJson.fail("缺少参数");
        try {
            this.getService().delete(student);
            hkJson.success();
        } catch (Exception e) {
            e.printStackTrace();
            hkJson.fail("删除失败");
        }
        return hkJson;
    }

    /*
     * @Author zxq
     * @Description //TODO admin 添加学生
     * @Date 2020/5/21
     **/
    @PostMapping(value = "/saveStudent")
    public HkJson saveStudent(@RequestBody HkCommonStudent student) {

        HkJson hkJson = new HkJson();

//        String sysid = UuidUtils.newTwentyKey();
//        student.setSysid(sysid);
//
        hkJson = this.getService().saveStudent(student);
        System.out.println(student);

        return hkJson;
    }

/*
     * @Author zxq
     * @Description //TODO admin 修改学生
     * @Date 2020/5/21
     **/
    @PostMapping(value = "/updateStudent")
    public HkJson updateStudent(@RequestBody HkCommonStudent student) {

        HkJson hkJson = new HkJson();

        hkJson = this.getService().updateStudent(student);
        return hkJson;
    }
    /*
     * @Author zxq
     * @Description //TODO admin 详情学生
     * @Date 2020/5/21
     **/
    @PostMapping(value = "/getStudent")
    public HkJson getStudent(@RequestBody HkCommonStudent entity) {
        HkJson hkJson = new HkJson();
        if (null == entity || StringUtils.isBlank(entity.getSysid())) {
            return hkJson.fail("传入参数为空");
        }
        try {
            HkCommonStudent info = this.getService().getEntity(entity.getSysid());
            hkJson.setData(info).success("查询成功");
        } catch (Exception e) {
            e.printStackTrace();
            hkJson.fail("查询失败");
        }
        return hkJson;
    }



}