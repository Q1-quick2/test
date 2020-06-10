package com.kx.diesel.common.entity;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

import com.kx.basic.entity.BasicEntity;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@SuppressWarnings("serial")
@Data
public class HkCommonStudent extends BasicEntity implements Serializable{

	private String sysid;	//主键
	private String name;	//学生姓名
	private String sex;	//性别
	private String teacher;	//所属老师
	@Override
	public String getId() {
		return this.sysid;
	}

	@Override
	public void setId(String id) {
		this.sysid = id;
	}

}