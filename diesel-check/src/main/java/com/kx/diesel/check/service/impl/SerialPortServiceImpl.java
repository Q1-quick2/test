package com.kx.diesel.check.service.impl;

import com.kx.basic.service.impl.BasicServiceImpl;
import com.kx.basic.util.HkJson;
import com.kx.basic.util.UuidUtils;
import com.kx.diesel.check.config.CommandConfig;
import com.kx.diesel.check.service.SerialPortService;
import com.kx.diesel.check.util.SerialTool;
import com.kx.diesel.road.entity.MdRoadCheckDevice;
import com.kx.diesel.road.entity.MdRoadCheckProcessData;
import com.kx.diesel.road.entity.MdRoadVehicleCheckRecord;
import com.kx.diesel.road.entity.MdRoadVehicleInfo;
import com.kx.diesel.road.mapper.MdRoadVehicleCheckRecordMapper;
import com.kx.diesel.road.mapper.MdRoadVehicleInfoMapper;
import com.kx.diesel.road.service.MdRoadCheckDeviceService;
import com.kx.diesel.road.service.MdRoadCheckProcessDataService;
import com.kx.security.utils.SecurityUtils;
import com.kx.system.entity.HkSystemSelect;
import com.kx.system.mapper.HkSystemSelectMapper;
import com.kx.system.service.HkSystemSelectService;
import gnu.io.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author Liumin
 * @date 2020-02-25 13:47
 */
@Service
public class SerialPortServiceImpl extends BasicServiceImpl<MdRoadCheckProcessData> implements SerialPortService {
  private static Logger logger = LoggerFactory.getLogger(SerialPortServiceImpl.class);
  @Autowired
  private CommandConfig commandConfig;

  @Autowired
  private MdRoadCheckProcessDataService mdRoadCheckProcessDataService;

  @Resource
  private HkSystemSelectMapper hkSystemSelectMapper;

  @Resource
  private MdRoadVehicleCheckRecordMapper mdRoadVehicleCheckRecordMapper;

  @Override
  public HkJson getRealTimeTest(Map<String,Object> params) {
    HkJson hkJson = new HkJson();
    SerialPort serialPort = null;
    List<String> smokeList = new ArrayList();
    // 检测 获取数据
    boolean checks = (boolean) params.get("checks");
    // 端口名称
    String smokeSerialPortName = (String) params.get("smokeSerialPortName");
    // 波特率
    String smokeBaudRate = (String) params.get("smokeBaudRate");
    // 烟度计获取频率
    String smokeFrequency = (String) params.get("smokeFrequency");
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    try {
      // 1、打开串口
      serialPort = SerialTool.openSerialPort(smokeSerialPortName, Integer.parseInt(smokeBaudRate));
      // 2、 设置监听
      smokeList = this.setSmokeListener(serialPort);
      // 3.1 仪器
      String responseStr = null;
      List<Map<String, Object>> list = new ArrayList<>();
      long times = (long) ((float)1 / Integer.parseInt(smokeFrequency) * 1000);
      while (true){
        // 3.2 发送读取数据命令
        SerialTool.sendData(serialPort,SerialTool.hexStringToBytes(commandConfig.getRealTimeData()));
        Thread.sleep(checks ? 100 : times);
        if(smokeList.isEmpty()){
          return hkJson.fail("通讯异常(无响应)，检查仪器是否连接正常");
        }
        if(smokeList != null){
          responseStr = smokeList.get(smokeList.size()-1);
          // 判断是否 正确应答
          if(responseStr.equals(commandConfig.getIllegalInstruction())){
            return hkJson.fail("通讯异常(未正确响应)，检查仪器是否正常");
          }
          // 根据响应值进行前缀进行分割
          String[] strSplit = responseStr.split("A6 ");
          if(strSplit.length > 0){
            for (int i = 0; i < strSplit.length; i++){
              if(StringUtils.isBlank(strSplit[i])){
                continue;
              }
              Map<String, Object> map = new HashMap<>();
              String[] data = strSplit[i].split(" ");
              // 吸收比 取一位小数
              map.put("dataNs", String.format("%.1f", (double) SerialTool.getChange(data[0] + data[1]) / 10));
              // 光吸收系数 取两位小数
              map.put("dataK", String.format("%.2f", (double) SerialTool.getChange(data[2] + data[3]) / 100));
              // 转速值
              map.put("dataRotatingSpeed", SerialTool.getChange(data[4] + data[5]));
              // 油温值  油温值-373K=100℃      ℃=K-273.15
              map.put("dataOilTemperature", SerialTool.getChange(data[6] + data[7]) - 273);

              // 检测结果
              map.put("checkResult", (double) SerialTool.getChange(data[2] + data[3]) / 100 < 1.20 );
              // 时间
              map.put("dateTime", sdf.format(new Date()));
              list.add(map);
            }
            if(checks || list.size() > 9){
              break;
            }
          }
        }
      }
      // 将测量数据添加到数据库里
      if(!checks){
        // 修改检测设备
        MdRoadVehicleCheckRecord record = new MdRoadVehicleCheckRecord();
        record.setSysid(params.get("checkId").toString());
        record.setContaminantDevice(params.get("deviceId").toString());
        mdRoadVehicleCheckRecordMapper.update(record);
        // 查询 额定转速值
        MdRoadVehicleCheckRecord mdRoadVehicleCheckRecord = mdRoadVehicleCheckRecordMapper.getEngineSpeed(params.get("checkId").toString());
        // 查询 烟度限值
        Map<String, Object> systemSelectCode = new HashMap<>();
        systemSelectCode.put("detail","0");
        systemSelectCode.put("selectKey","SMOKE_LIMIT");
        List<HkSystemSelect> hkSystemSelect = hkSystemSelectMapper.findSelect(systemSelectCode);
        if (hkSystemSelect.isEmpty()){
          return hkJson.setData(list).fail("烟度限值异常，本次数据将不存储");
        }
        // 添加到数据库
        for(Map<String, Object> item : list){
          MdRoadCheckProcessData data = new MdRoadCheckProcessData();
          data.setSysid(UuidUtils.newTwentyKey());
          data.setCheckId((String) params.get("checkId"));
          data.setCheckDate(sdf.parse((String) item.get("dateTime")));
          data.setCreateBy(SecurityUtils.getUserId());
          // 每一工况时间
          data.setEachDate(new Date());
          // 转速
          data.setEngineSpeed((Integer) item.get("dataRotatingSpeed"));
          // 光吸收系数K
          data.setLightAbsorption(Double.valueOf(item.get("dataK").toString()));
          // 吸收比
          data.setSmokeValue(Double.valueOf(item.get("dataNs").toString()));

          data.setLimitValue(Double.valueOf(hkSystemSelect.get(0).getSelectCode()));
          mdRoadCheckProcessDataService.insert(data);
          // 烟度限值
          item.put("limitValue",data.getLimitValue());
          // 额定转速
          item.put("engineSpeed",mdRoadVehicleCheckRecord.getEngineSpeed());
        }
      }
      hkJson.setData(list).success("仪器正常");
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (UnsupportedCommOperationException e) {
      // 串口通讯异常
      e.printStackTrace();
      hkJson.fail("通讯异常，检查烟度计仪器是否正常");
    } catch (NoSuchPortException e) {
      //找不到串口的情况下抛出该异常
      e.printStackTrace();
      hkJson.fail("找不到“" + smokeSerialPortName + "”该串口");
    } catch (PortInUseException e) {
      //如果因为端口被占用而导致打开失败，则抛出该异常
      e.printStackTrace();
      hkJson.fail("端口“" + smokeSerialPortName + "”被占用,导致打开失败");
    } catch (UnsatisfiedLinkError e) {
      e.printStackTrace();
      hkJson.fail("该串口不能进行数据检测！");
    } catch (Exception e) {
      e.printStackTrace();
      hkJson.fail("程序异常，请联系管理员！");
    }finally {
      SerialTool.closeSerialPort(serialPort);
    }
    return hkJson;
  }

  @Override
  public HkJson getFreeAcceleration(Map<String, Object> params) {
    HkJson hkJson = new HkJson();
    // 返回值封装的数据类型
    Map<String, Object> map = new HashMap<>();

    // -------------------------------------烟度计 start ---------------------------------------
    List<String> smokeList = new ArrayList();
    // 检测 获取数据
    boolean checks = (boolean) params.get("checks");
    // 烟度计 端口名称
    String smokeSerialPortName = (String) params.get("smokeSerialPortName");
    // 烟度计 波特率
    String smokeBaudRate = (String) params.get("smokeBaudRate");
    // 烟度计 获取频率
    String smokeFrequency = (String) params.get("smokeFrequency");
    if(StringUtils.isNotBlank(smokeSerialPortName) && StringUtils.isNotBlank(smokeSerialPortName)
      && StringUtils.isNotBlank(smokeFrequency)){
      SerialPort serialPort = null;
      try {
        // 1、打开烟度计 串口
        serialPort = SerialTool.openSerialPort(smokeSerialPortName, Integer.parseInt(smokeBaudRate));
        // 2、设置烟度计监听
        smokeList = this.setSmokeListener(serialPort);
        List<Map<String, Object>> list = new ArrayList<>();
        // 3.3 发送 取自由加速命令
        long times = (long) ((float)1 / Integer.parseInt(smokeFrequency) * 1000);
        while (true){
          SerialTool.sendData(serialPort,SerialTool.hexStringToBytes(commandConfig.getFreeAcceleration()));
          Thread.sleep(checks ? 100 : times);
          if(smokeList.isEmpty()){
            return hkJson.fail("通讯异常（仪器未响应），检查仪器是否连接正常");
          }
          String responseStr = smokeList.get(smokeList.size() - 1);
          // 判断是否 正确应答
          if(responseStr.equals(commandConfig.getIllegalInstruction())){
            return hkJson.fail("通讯异常(未得到仪器响应)，检查仪器是否正常");
          }
          // 将检测数据返回到hkjson
          responseStr = smokeList.get(smokeList.size()-1);
          // 根据响应值进行前缀进行分割
          String[] strSplit = responseStr.split("A7 ");
          if(strSplit.length > 0){
            for (int i = 0; i < strSplit.length; i++){
              if(StringUtils.isBlank(strSplit[i])){
                continue;
              }
              Map<String, Object> mapData = new HashMap<>();
              String[] data = strSplit[i].split(" ");
              // 吸收比 取一位小数
              mapData.put("dataNs", String.format("%.1f", (double) SerialTool.getChange(data[0] + data[1]) / 10));
              // 光吸收系数 取两位小数
              mapData.put("dataK", String.format("%.2f", (double) SerialTool.getChange(data[2] + data[3]) / 100));
              // 检测结果
              mapData.put("checkResult", (double) SerialTool.getChange(data[2] + data[3]) / 100 < 1.20 );
              // 时间
              mapData.put("dateTime", LocalDateTime.now());
              list.add(mapData);
              map.put("smokeList",list);
            }
            if(list.size() > 9){
              break;
            }
          }
        }
        hkJson.setData(map).success("响应成功！");
      } catch (InterruptedException e){
        e.printStackTrace();
        return hkJson.fail("延迟线程异常！");
      }catch (UnsupportedCommOperationException e) {
        // 串口通讯异常
        e.printStackTrace();
        hkJson.fail("通讯异常，检查烟度计仪器是否正常");
      } catch (NoSuchPortException e) {
        //找不到串口的情况下抛出该异常
        e.printStackTrace();
        hkJson.fail("找不到“" + smokeSerialPortName + "”该串口");
      } catch (PortInUseException e) {
        //如果因为端口被占用而导致打开失败，则抛出该异常
        e.printStackTrace();
        hkJson.fail("端口“" + smokeSerialPortName + "”被占用,导致打开失败");
      } catch (Exception e) {
        e.printStackTrace();
        hkJson.fail("程序异常，请联系管理员！");
      }finally {
        SerialTool.closeSerialPort(serialPort);
      }

    }
    //  ------------------------------------- end ---------------------------------------

    //  ------------------------------------- 转速仪 start ---------------------------------------
    /*List<String> tachometerList = new ArrayList<String>();
    // 转速仪 端口名称
    String tachometerSerialPortName = (String) params.get("tachometerSerialPortName");
    // 转速仪 波特率
    String tachometerBaudRate = (String) params.get("tachometerBaudRate");
    // 转速仪 获取频率
    String tachometerFrequency = (String) params.get("tachometerFrequency");
    if(StringUtils.isNotBlank(tachometerSerialPortName) && StringUtils.isNotBlank(tachometerBaudRate)
      && StringUtils.isNotBlank(tachometerFrequency)){
      // 1、打开串口
      final SerialPort serialPortTachometer = SerialTool.openSerialPort(tachometerSerialPortName, Integer.parseInt(tachometerBaudRate));
      // 2、 设置烟度计监听
      tachometerList = this.setTachometerListener(serialPortTachometer);
      // 3、发送指令
      boolean isTermination = true;
      while (isTermination){
        try {
          Thread.sleep(Integer.parseInt(tachometerFrequency));
          if(tachometerList.size() > 12){
            map.put("tachometerList",tachometerList.subList(tachometerList.size()-11,tachometerList.size()-1));
            isTermination = false;
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      // 4、关闭串口
      SerialTool.closeSerialPort(serialPortTachometer);
    }*/
    // 如果前台未正确传值时 不执行任何操作
    if(map.isEmpty()){
      hkJson.fail("当前串口值参数配置错误！");
      return hkJson;
    }
    return hkJson;
  }

  @Override
  public HkJson getAllData(Map<String, Object> params) {
    HkJson hkJson = new HkJson();
    List<String> smokeList = new ArrayList();
    SerialPort serialPort = null;
    // 烟度计 端口名称
    String smokeSerialPortName = (String) params.get("smokeSerialPortName");
    // 烟度计 波特率
    String smokeBaudRate = (String) params.get("smokeBaudRate");
    try {
      // 1、打开串口
      serialPort = SerialTool.openSerialPort(smokeSerialPortName, Integer.parseInt(smokeBaudRate));
      // 2、 设置监听
      smokeList = this.setSmokeListener(serialPort);
      String responseStr = null;
      List<Map<String, Object>> list = new ArrayList<>();
      while (true){
        // 3.2 发送读取数据命令
        SerialTool.sendData(serialPort,SerialTool.hexStringToBytes(commandConfig.getAllData()));
        Thread.sleep(100);
        // 判断是否 正确应答
        if(responseStr.equals(commandConfig.getIllegalInstruction())){
          return hkJson.fail("通讯异常(未正确响应)，检查仪器是否正常");
        }
        if(smokeList != null){
          responseStr = smokeList.get(smokeList.size()-1);
          // 根据响应值进行前缀进行分割
          String[] strSplit = responseStr.split("AE ");
          if(strSplit.length > 0){
            for (int i = 0; i < strSplit.length; i++){
              if(StringUtils.isBlank(strSplit[i])){
                continue;
              }
              Map<String, Object> map = new HashMap<>();
              String[] data = strSplit[i].split(" ");
              // 吸收比 取一位小数
              map.put("dataNs", String.format("%.1f", (double) SerialTool.getChange(data[0] + data[1]) / 10));
              // 光吸收系数 取两位小数
              map.put("dataK", String.format("%.2f", (double) SerialTool.getChange(data[2] + data[3]) / 100));
              // 转速值
              map.put("dataRotatingSpeed", SerialTool.getChange(data[4] + data[5]));
              // 油温值  油温值-373K=100℃      ℃=K-273.15
              map.put("dataOilTemperature", SerialTool.getChange(data[6] + data[7]) - 273);
              // 压力值
              map.put("dataPressure", SerialTool.getChange(data[8] + data[9]));
              // 温度值 需要解析
              map.put("dataTemperature", SerialTool.getChange(data[10] + data[11]));
              // 湿度值
              map.put("dataHumidity", SerialTool.getChange(data[12] + data[13]));
              list.add(map);
            }
            break;
          }
        }
      }
      hkJson.setData(list).success("仪器正常");
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (UnsupportedCommOperationException e) {
      // 串口通讯异常
      e.printStackTrace();
      hkJson.fail("通讯异常，检查烟度计仪器是否正常");
    } catch (NoSuchPortException e) {
      //找不到串口的情况下抛出该异常
      e.printStackTrace();
      hkJson.fail("找不到“" + smokeSerialPortName + "”该串口");
    } catch (PortInUseException e) {
      //如果因为端口被占用而导致打开失败，则抛出该异常
      e.printStackTrace();
      hkJson.fail("端口“" + smokeSerialPortName + "”被占用,导致打开失败");
    } catch (Exception e) {
      e.printStackTrace();
      hkJson.fail("程序异常，请联系管理员！");
    }finally {
      SerialTool.closeSerialPort(serialPort);
    }
    return hkJson;
  }

  @Override
  public HkJson getFreeAccelerationStatus(Map<String, Object> params) {
    HkJson hkJson = new HkJson();
    List<String> smokeList = new ArrayList();
    // 端口名称
    String smokeSerialPortName = (String) params.get("smokeSerialPortName");
    // 波特率
    String smokeBaudRate = (String) params.get("smokeBaudRate");
    SerialPort serialPort = null;
    try {
      // 1、打开串口
      serialPort = SerialTool.openSerialPort(smokeSerialPortName, Integer.parseInt(smokeBaudRate));
      // 2、 设置监听
      smokeList = this.setSmokeListener(serialPort);
      String responseStr = null;
      List<Map<String, Object>> list = new ArrayList<>();
      // 3.2 发送去自由加速试验状态
      SerialTool.sendData(serialPort,SerialTool.hexStringToBytes(commandConfig.getFreeAccelerationStatus()));
      Thread.sleep(100);
      if(smokeList.isEmpty()){
        return hkJson.fail("仪器未响应，检查仪器是否正常");
      }
      // 判断是否 正确应答
      responseStr = smokeList.get(smokeList.size() - 1);
      if(responseStr.equals(commandConfig.getIllegalInstruction())){
        return hkJson.fail("通讯异常(未正确响应)，检查仪器是否正常");
      }
      if(smokeList != null) {
        responseStr = smokeList.get(smokeList.size() - 1);
        // 根据响应值进行前缀进行分割
        String[] strSplit = responseStr.split("A5 ");
        if (strSplit.length > 0) {
          for (int i = 0; i < strSplit.length; i++) {
            if (StringUtils.isBlank(strSplit[i])) {
              continue;
            }
            String[] data = strSplit[i].split(" ");
            // 状态码
            String statusCode = data[0];
            if(statusCode.equals("00")){
              hkJson.setData("仪器初始状态货结束测量状态，此时可进行启动！");
            }
            else if(statusCode.equals("02") || statusCode.equals("01")){
              hkJson.setData("将取样探头放入清洁处准备校准！");
            }
            else if(statusCode.equals("03") || statusCode.equals("04")){
              hkJson.setData("仪器正在校准！");
            }
            else if(statusCode.equals("05")){
              hkJson.setData("仪器已完成校准！");
            }
            else if(statusCode.equals("06") || statusCode.equals("07")){
              hkJson.setData("正在检测初始状态！");
            }
            else if(statusCode.equals("08") || statusCode.equals("0B")){
              hkJson.setData("车辆可加速！");
            }
            else if(statusCode.equals("0C") || statusCode.equals("0E")){
              hkJson.setData("车辆可减速！");
            }
            else if(statusCode.equals("0F")){
              hkJson.setData("可获取实验结果！");
            }
            else if(statusCode.equals("FF")){
              hkJson.setData("仪器出现故障或错误，需进行检查！");
            }
          }
        }
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (UnsupportedCommOperationException e) {
      // 串口通讯异常
      e.printStackTrace();
      hkJson.fail("通讯异常，检查烟度计仪器是否正常");
    } catch (NoSuchPortException e) {
      //找不到串口的情况下抛出该异常
      e.printStackTrace();
      hkJson.fail("找不到“" + smokeSerialPortName + "”该串口");
    } catch (PortInUseException e) {
      //如果因为端口被占用而导致打开失败，则抛出该异常
      e.printStackTrace();
      hkJson.fail("端口“" + smokeSerialPortName + "”被占用,导致打开失败");
    } catch (Exception e) {
      e.printStackTrace();
      hkJson.fail("程序异常，请联系管理员！");
    }finally {
      SerialTool.closeSerialPort(serialPort);
    }
    hkJson.success();
    return hkJson;
  }

  /**
   * 烟度计监听
   * @param serialPort
   * @return
   */
  private List<String> setSmokeListener(SerialPort serialPort){
    List<String> smokeList = new ArrayList();
    try {
      //2、进行监听 设置串口的listener
      SerialTool.setListenerToSerialPort(serialPort, new SerialPortEventListener() {
        @Override
        public void serialEvent(SerialPortEvent arg0) {
          // 解决数据断行
          try {
            Thread.sleep(1);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          if(arg0.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            // 数据通知
            byte[] bytes = SerialTool.readData(serialPort);
            try {
              //logger.info("烟度计收到的数据长度："+bytes.length);
              String info= SerialTool.bytesToHexString(bytes);
              smokeList.add(info);
              logger.info("烟度计收到的数据："+ info);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      });

    } catch (TooManyListenersException e) {
      // 监听事件异常
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return smokeList;
  }

  /**
   * 转速仪监听
   * @param serialPort
   * @return
   */
  private List<String> setTachometerListener(SerialPort serialPort){
    List<String> smokeList = new ArrayList();
    try {
      //2、进行监听 设置串口的listener
      SerialTool.setListenerToSerialPort(serialPort, new SerialPortEventListener() {
        @Override
        public void serialEvent(SerialPortEvent arg0) {
          // 解决数据断行
          try {
            Thread.sleep(1);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          if(arg0.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            // 数据通知
            byte[] bytes = SerialTool.readData(serialPort);
            try {
              // logger.info("转速仪收到的数据长度："+bytes.length);
              String info= new String(bytes,"GBK");
              smokeList.add(info);
              logger.info("转速仪收到的数据："+ info);
            } catch (UnsupportedEncodingException e) {
              e.printStackTrace();
            }
          }
        }
      });

    } catch (TooManyListenersException e) {
      // 监听事件异常
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return smokeList;
  }
}
