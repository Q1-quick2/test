package com.kx.diesel.check.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * @author Liumin
 * @date 2020-02-24 10:33
 */
@Component
@Data
@ConfigurationProperties(prefix = "command")
@PropertySource(value = "classpath:command.properties")
public class CommandConfig {
  // 选择测量方式
  private String currentMeasurementMethod;

  // 校准（实时测试方式）
  private String calibration;

  // 开始或继续自由加速试验
  private String startFreeAcceleration;

  // 结束或终止自由加速试验
  private String endFreeAcceleration;

  // 取实时测试数据
  private String realTimeData;

  // 取自由加速实验数据
  private String freeAcceleration;

  // 清除最大值
  private String clearMaxData;

  // 取最大值 实时测试
  private String maxData;

  // 取所有数据
  private String allData;

 // 非法指令
  private String illegalInstruction;

 // 自由加速试验状态
  private String freeAccelerationStatus;

}
