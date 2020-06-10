package com.kx.diesel.check.util;

import gnu.io.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TooManyListenersException;

/**
 * @author Liumin
 * @date 2020-02-26 17:52
 */
public class SerialTool {
  @SuppressWarnings("unchecked")
  public static List<String> getSerialPortList() {
    List<String> systemPorts = new ArrayList<>();
    //获得系统可用的端口
    Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();
    while (portList.hasMoreElements()) {
      String portName = portList.nextElement().getName();//获得端口的名字
      systemPorts.add(portName);
    }
    return systemPorts;
  }

  // 打开串口  *** 一系列情况，不断重写传递参数 ***
  public static SerialPort openSerialPort(String serialPortName)
    throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException {
    SerialParameter parameter = new SerialParameter(serialPortName);
    return openSerialPort(parameter);
  }

  public static SerialPort openSerialPort(String serialPortName, int baudRate)
    throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException {
    SerialParameter parameter = new SerialParameter(serialPortName, baudRate);
    return openSerialPort(parameter);
  }

  public static SerialPort openSerialPort(String serialPortName, int baudRate, int timeout)
    throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException {
    SerialParameter parameter = new SerialParameter(serialPortName, baudRate);
    return openSerialPort(parameter, timeout);
  }

  public static SerialPort openSerialPort(SerialParameter parameter)
    throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException {
    return openSerialPort(parameter, 2000);
  }

  public static SerialPort openSerialPort(SerialParameter parameter, int timeout)
    throws PortInUseException, UnsupportedCommOperationException, NoSuchPortException {
    //通过端口名称得到端口
    CommPortIdentifier portIdentifier = null;
    try {
      portIdentifier = CommPortIdentifier.getPortIdentifier(parameter.getSerialPortName());
    } catch (NoSuchPortException e) {
      e.printStackTrace();
    }
    //打开端口，（自定义名字，打开超时时间）
    CommPort commPort = portIdentifier.open(parameter.getSerialPortName(), timeout);
    //判断是不是串口
    if (commPort instanceof SerialPort) {
      SerialPort serialPort = (SerialPort) commPort;
      //设置串口参数（波特率，数据位8，停止位1，校验位无）
      serialPort.setSerialPortParams(parameter.getBaudRate(), parameter.getDataBits(), parameter.getStopBits(), parameter.getParity());
      System.out.println("开启串口成功，串口名称：" + parameter.getSerialPortName());
      return serialPort;
    } else {
      //是其他类型的端口
      throw new NoSuchPortException();
    }
  }
  // *** 最后一个即是最终结果 ***

  // 关闭串口
  public static void closeSerialPort(SerialPort serialPort) {
    if (serialPort != null) {
      serialPort.close();
      System.out.println("关闭了串口：" + serialPort.getName());
    }
  }

  // 向串口发送数据
  public static void sendData(SerialPort serialPort, byte[] data) {
    OutputStream os = null;
    try {
      //获得串口的输出流
      os = serialPort.getOutputStream();
      os.write(data);
      os.flush();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (os != null) {
          os.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  // 从串口读取数据
  public static byte[] readData(SerialPort serialPort) {
    InputStream is = null;
    byte[] bytes = null;
    try {
      //获得串口的输入流
      is = serialPort.getInputStream();
      //获得数据长度
      int bufflenth = is.available();
      while (bufflenth != 0) {
        //初始化byte数组
        bytes = new byte[bufflenth];
        is.read(bytes);
        bufflenth = is.available();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (is != null) {
          is.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return bytes;
  }

  // 给串口设置监听
  public static void setListenerToSerialPort(SerialPort serialPort, SerialPortEventListener listener) throws TooManyListenersException {
    //给串口添加事件监听
    serialPort.addEventListener(listener);
    //串口有数据监听
    serialPort.notifyOnDataAvailable(true);
    //中断事件监听
    serialPort.notifyOnBreakInterrupt(true);
  }


  /**
   * 数组转换成十六进制字符串
   * @param bArray
   * @return HexString
   */
  public static final String bytesToHexString(byte[] bArray) {
    StringBuffer sb = new StringBuffer(bArray.length);
    String sTemp;
    for (int i = 0; i < bArray.length; i++) {
      sTemp = Integer.toHexString(0xFF & bArray[i]);
      if (sTemp.length() < 2)
        sb.append(0);
        sb.append(sTemp.toUpperCase());
        if((bArray.length - 1) != i){
          sb.append(" ");
        }
    }
    return sb.toString();
  }

  // 修改10进制数据
  public static int getChange(String data){
    int sum = 0;
    for(int i=0;i<data.length();i++)
    {
      int m=data.charAt(i);//将输入的十六进制字符串转化为单个字符
      int num=m>='A'?m-'A'+10:m-'0';//将字符对应的ASCII值转为数值
      sum+=Math.pow(16, data.length()-1-i)*num;
    }
    return sum;
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


