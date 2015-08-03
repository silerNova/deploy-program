package com.deploy.program.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class TomcatUtil {

	public static boolean createFiles(String catalinaHome, String catalinaBase) {
		boolean flag = false;
		if (catalinaBase == null || catalinaBase.isEmpty()) {
			throw new NullPointerException("tomcat实例路径为空, 请检查!");
		}
		File logFile = new File(catalinaBase + File.separator + "logs");
		if (!logFile.exists()) {
			logFile.mkdir();
		}
		File tempFile = new File(catalinaBase + File.separator + "temp");
		if (!tempFile.exists()) {
			tempFile.mkdir();
		}
		File workFile = new File(catalinaBase + File.separator + "work");
		if (!workFile.exists()) {
			workFile.mkdir();
		}
		File binFile = new File(catalinaBase + File.separator + "bin");
		if (!binFile.exists()) {
			if (binFile.mkdir()) {
				// 将脚本文件写入bin目录中
				String systemName = System.getProperty("os.name");
				if (systemName.startsWith("Windows")) {
					writeScripts(catalinaHome, binFile, "start.bat", "stop.bat", true);
				} else {
					writeScripts(catalinaHome, binFile, "start.sh", "stop.sh", false);
				}
			}
		}
		return flag;
	}
	
	@SuppressWarnings("unchecked")
	public static boolean changePort(File serverFile, String port) {
		boolean flag = false;
		// 先备份配置文件, 然后再修改备份配置文件, 另存为新的配置文件
		if (!serverFile.exists()) {
			throw new NullPointerException("实例的配置文件不存在, 请检查!");
		}
		String backupFileName = serverFile.getParent() + File.separator + serverFile.getName() + ".bak";
		File bakFile = new File(backupFileName);
		// 先将数据备份
		serverFile.renameTo(bakFile);
		// 对备份文件进行读取, 并修改, 另存为新的配置文件
		SAXReader sax = new SAXReader();
		Document doc = null;
		try {
			doc = sax.read(bakFile);
			// 根节点为: Server
			Element rootElement = doc.getRootElement();
			// 获取根节点的属性
			String shutDown = rootElement.attributeValue("shutdown");
			// 如果shutdown的值是SHUTDOWN, 则修改关闭端口
			if (null != shutDown && "SHUTDOWN".equalsIgnoreCase(shutDown)) {
				// 获取端口逻辑, 随机在60000 - 65535之间选一个
				int shutPortValue = (int) (Math.random() * 5535) + 60000;
				Attribute shutPortAttribute = rootElement.attribute("port");
				shutPortAttribute.setValue(String.valueOf(shutPortValue));
			}
			List<Element> serviceElements = rootElement.elements("Service");
			if (serviceElements != null && serviceElements.size() > 0) {
				for (Element serviceElement : serviceElements) {
					String nameAttribute = serviceElement.attribute("name").getValue();
					if (!"catalina".equalsIgnoreCase(nameAttribute)) {
						continue;
					} else {
						List<Element> connElements = serviceElement.elements("Connector");
						if (connElements != null && connElements.size() > 0) {
							for (Element connElement : connElements) {
								String protocol = connElement.attributeValue("protocol");
								if ("HTTP/1.1".equals(protocol)
										|| "HTTP/1.0".equals(protocol)) {
									String sslEnabled = connElement.attributeValue("SSLEnabled");
									if (!"true".equals(sslEnabled)) {
										// 进行配置文件端口的修改
										Attribute portAttribute = connElement.attribute("port");
										portAttribute.setValue(port);
									}
								}
							}
							// 将修改内容写入xml文件
							XMLWriter writer = null;
							try {
								writer = new XMLWriter(new FileWriter(serverFile));
								writer.write(doc);
								writer.flush();
								flag = true;
							} catch (IOException e) {
								e.printStackTrace();
							} finally {
								if (writer != null) {
									try {
										writer.close();
									} catch (IOException e) {
										e.printStackTrace();
									}
									writer = null;
								}
							}
						}
					}
				}
			}
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return flag;
	}

	public static boolean writeRootConf(String rootConfigFile, String deployPath) {
		boolean flag = false;
		File file = new File(rootConfigFile);
		if (!file.exists()) {
			// 判断他的父目录是否存在
			File parent = file.getParentFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}
			try {
				file.createNewFile();
				file.setExecutable(true);
				file.setReadable(true);
				file.setWritable(true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		StringBuilder sb = new StringBuilder(100);
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
		  .append("<Context path=\"/\" docBase=")
		  .append("\"")
		  .append(deployPath)
		  .append("\">")
		  .append("</Context>");
		OutputStream out = null;
		BufferedWriter bw = null;
		try {
			out = new FileOutputStream(file);
			bw = new BufferedWriter(new OutputStreamWriter(out));
			bw.write(sb.toString());
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bw != null) {
				try {
					bw.close();
					bw = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (out != null) {
				try {
					out.close();
					out = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return flag;
	}
	
	public static boolean writeScripts(String catalinaHome, File binPath,
			String startFileName, String stopFileName, boolean isWindows) {
		boolean flag = false;
		String catalinaBase = binPath.getParent();
		File startFile = new File(binPath, startFileName);
		if (!startFile.exists()) {
			try {
				startFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			startFile.setExecutable(true);
			startFile.setReadable(true);
			startFile.setWritable(true);
		}
		File stopFile = new File(binPath, stopFileName);
		if (!stopFile.exists()) {
			try {
				stopFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			stopFile.setExecutable(true);
			stopFile.setReadable(true);
			stopFile.setWritable(true);
		}
		
		// 启动脚本字符串
		StringBuilder startStr = new StringBuilder(300);
		if (isWindows) {
			startStr.append("@echo off").append("\r\n")
			        .append("set CATALINA_HOME=")
			        .append(catalinaHome).append("\r\n")
			        .append("set CATALINA_BASE=")
			        .append(catalinaBase).append("\r\n")
			        .append("%CATALINA_HOME%\\bin\\startup.bat -config %CATALINA_BASE%\\conf\\server.xml start");
		} else {
			startStr.append("#!/bin/bash\n")
		        .append("export CATALINA_HOME=")
		        .append(catalinaHome).append("\n")
		        .append("export CATALINA_BASE=")
		        .append(catalinaBase).append("\n")
		        .append("export JAVA_HOME=")
		        .append(System.getenv("JAVA_HOME")).append("\n")
		        .append("export JAVA_BIN=").append(System.getenv("JAVA_HOME"))
		        .append(File.separator).append("bin").append("\n")
		        .append("export PATH=/usr/kerberos/sbin:/usr/kerberos/bin:/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin:/root/bin:/bin\n")
		        .append("export CLASSPATH=.:/lib/dt.jar:/lib/tools.jar\n")
		        .append("export  JAVA_OPTS=\"-Djava.library.path=/usr/local/lib -server -Xms1024m -Xmx1024m -XX:MaxPermSize=256m -Djava.awt.headless=true -Dsun.net.client.defaultConnectTimeout=60000 -Dsun.net.client.defaultReadTimeout=60000 -Djmagick.systemclassloader=no -Dnetworkaddress.cache.ttl=300 -Dsun.net.inetaddr.ttl=300\"\n")
		        .append("export JAVA_HOME JAVA_BIN PATH CLASSPATH JAVA_OPTS\n")
		        .append("$CATALINA_HOME/bin/startup.sh -config $CATALINA_BASE/conf/server.xml");
		}
		
		// 停止脚本字符串
		StringBuilder stopStr = new StringBuilder();
		if (isWindows) {
			stopStr.append("@echo off").append("\r\n")
			        .append("set CATALINA_HOME=")
			        .append(catalinaHome).append("\r\n")
			        .append("set CATALINA_BASE=")
			        .append(catalinaBase).append("\r\n")
			        .append("%CATALINA_HOME%\\bin\\shutdown.bat -config %CATALINA_BASE%\\conf\\server.xml start");
				} else {
			stopStr.append("#!/bin/bash\n")
		       .append("export CATALINA_HOME=")
		       .append(catalinaHome).append("\n")
		       .append("export CATALINA_BASE=")
		       .append(catalinaBase).append("\n")
		       .append("$CATALINA_HOME/bin/shutdown.sh -config $CATALINA_BASE/conf/server.xml\n")
		       .append("ps -aef | grep java|grep ")
		       .append("\"")
		       .append(catalinaBase)
		       .append("\"")
		       .append("| grep -v grep | sed 's/ [ ]*/:/g' |cut -d: -f2|kill -9 `cat`");
		}
		OutputStream out = null;
		BufferedWriter bw = null;
		
		try {
			out = new FileOutputStream(startFile);
			bw = new BufferedWriter(new OutputStreamWriter(out));
			bw.write(startStr.toString());
			bw.flush();
			bw.close();
			out.close();
			
			out = new FileOutputStream(stopFile);
			bw = new BufferedWriter(new OutputStreamWriter(out));
			bw.write(stopStr.toString());
			bw.flush();
			flag = true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				bw = null;
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				out = null;
			}
			
		}
		return flag ;
	}
}
