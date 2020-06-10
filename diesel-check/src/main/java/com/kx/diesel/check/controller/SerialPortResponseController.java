package com.kx.diesel.check.controller;

import com.kx.basic.util.HkJson;
import com.kx.diesel.check.util.SerialTool;
import gnu.io.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * @author Liumin
 * @date 2020-02-14 13:49
 */

@RestController
@RequestMapping("/serialPortResponseController")
public class SerialPortResponseController {

  private static Logger logger = LoggerFactory.getLogger(SerialPortResponseController.class);
  /**
   * @Description: 响应接口
   * @Author: liumin
   * @Date: 2020-02-14
   */
  @PostMapping(value = "/openSerialPortListener")
  public HkJson getSerialPortList(@RequestBody Map<String,Object> params) {
    HkJson hkJson = new HkJson();
    try {
      //开启端口COM2，波特率9600，根据自己的情况更改
      final SerialPort serialPort = SerialTool.openSerialPort("COM2", 9600);
      //设置串口的listener
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
              System.out.println("收到的数据长度："+bytes.length);
              String info= SerialTool.bytesToHexString(bytes);
              // 握手命令
              if(info.equals("A2 5E")){
                String str="A2 5E";
                SerialTool.sendData(serialPort,hexStringToBytes(str));
              }
              // 实时响应数据
              else if(info.equals("A6 5A")){
                //for (int i = 0; i < 10; i++) {
                String str= "A6 01 F4 00 A1 0B B8 01 75 8B";
                SerialTool.sendData(serialPort,hexStringToBytes(str));
                //}
              }
              // 零点校准命令
              else if(info.equals("A8 58")){
                String str="A8 58";
                SerialTool.sendData(serialPort,hexStringToBytes(str));
              }
              // 满值点校准命
              else if(info.equals("A7 59")){
                String str="A7 01 F4 00 A1 57";
                SerialTool.sendData(serialPort,hexStringToBytes(str));
              }
              // 自由加速状态
              else if(info.equals("A5 5B")){
                String str="A5 01 57";
                SerialTool.sendData(serialPort,hexStringToBytes(str));
              }
              System.out.println("收到的数据："+ info);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      });
    } catch (UnsupportedCommOperationException e) {
      // 串口通讯异常
      e.printStackTrace();
    } catch (TooManyListenersException e) {
      // 监听事件异常
      e.printStackTrace();
    } catch (NoSuchPortException e) {
      //找不到串口的情况下抛出该异常
      e.printStackTrace();
    } catch (PortInUseException e) {
      //如果因为端口被占用而导致打开失败，则抛出该异常
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
    hkJson.success("打开成功");
    return hkJson;
  }

  //将输入的16进制string转成字节
  public static byte[] hexStringToBytes(String hexString) {
    if (hexString == null || hexString.equals("")) {
      return null;
    }
    hexString = hexString.toUpperCase();
    String[] hexStrings = hexString.split(" ");
    byte[] bytes = new byte[hexStrings.length];
    for (int i = 0; i < hexStrings.length; i++)
    {
      char[] hexChars = hexStrings[i].toCharArray();
      bytes[i] = (byte) (charToByte(hexChars[0]) << 4 | charToByte(hexChars[1]));
    }
    return bytes;
  }
  private static byte charToByte(char c) {
    return (byte) "0123456789ABCDEF".indexOf(c);
  }
}
