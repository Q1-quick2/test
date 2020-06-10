package com.kx.diesel.check.service;

import com.kx.basic.util.HkJson;

import java.util.Map;

/**
 * @author Liumin
 * @date 2020-02-25 13:47
 */
public interface SerialPortService {
  // 实时数据获取
  //HkJson getRealTimeTest(boolean checks, String smokeSerialPortName, String smokeBaudRate, String smokeFrequency);
  HkJson getRealTimeTest(Map<String, Object> params);

  // 自由加速
  HkJson getFreeAcceleration(Map<String, Object> params);

  // 获取所有数据
  HkJson getAllData(Map<String, Object> params);

  // 获取自由加速状态
  HkJson getFreeAccelerationStatus(Map<String, Object> params);

}
