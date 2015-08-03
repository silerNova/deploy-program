package com.deploy.program.launcher;

import java.io.File;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.deploy.program.bean.Root;
import com.deploy.program.deploy.factory.AbstractDeployFactory;
import com.deploy.program.deploy.factory.extend.ServiceDeployFactory;
import com.deploy.program.deploy.factory.extend.WarDeployFactory;
import com.deploy.program.deploy.factory.extend.WorkerDeployFactory;
import com.deploy.program.deploy.interfaces.Deployer;
import com.deploy.program.util.XMLUtil;

public class AutoDeployLauncher {
	
	private transient static Logger LOGGER = null;
	
	static {
		// 获取classpath下log4j.properties配置文件的路径
		String logConfigFilePath = AutoDeployLauncher.class.getClassLoader().getResource("log4j.properties").getPath();
		if (null == logConfigFilePath || logConfigFilePath.isEmpty()) {
			logConfigFilePath = Thread.currentThread().getContextClassLoader().getResource("log4j.properties").getPath();
			if (null == logConfigFilePath || logConfigFilePath.isEmpty()) {
				throw new NullPointerException("获取log4j日志配置文件失败。");
			}
		}
		// 加载日志配置，并定时检查配置文件变化
		PropertyConfigurator.configureAndWatch(logConfigFilePath, 5000);
		LOGGER = LoggerFactory.getLogger(AutoDeployLauncher.class);
	}
	
	public static void main(String[] args) {
		if (false) {
			System.setProperty("configFilePath", "F:\\test\\config");
		}
		// 配置文件名称由参数传入
		if (args == null || args.length != 1) {
			if (null == args) {
				if (LOGGER.isErrorEnabled()) {
					LOGGER.error("传入的参数为空,请检查");
				}
			}
			if (args.length != 1) {
				if (LOGGER.isErrorEnabled()) {
					LOGGER.error("传入的参数的长度不为1, 长度为: " + args.length);
				}
			}
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error("传入参数非法, 需要传入配置文件的名称,请检查!");
			}
			throw new IllegalArgumentException("传入参数为空, 需要传入配置文件的名称,请检查!");
		}
		// 读取配置文件路径, 配置文件路径设置在启动上下文中
		String deployConfigFile = System.getProperty("configFilePath");
		if (deployConfigFile == null || deployConfigFile.isEmpty()) {
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error("系统没有找到部署配置文件,程序退出执行。 请检查!");
			}
			System.exit(0);
		}
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("获取配置文件: " + deployConfigFile + File.separator + args[0]);
		}
		// 读取配置文件, 并解析里面的内容
		Root root = XMLUtil.readConfigFile(deployConfigFile + File.separator + args[0]);
		if (root == null) {
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error("读取系统配置文件失败,程序退出执行。请检查!");
			}
			System.exit(0);
		}
		// 判断需要部署的是什么类型的数据
		AbstractDeployFactory factory = null;
		String deployType = root.getType();
		if ("war".equalsIgnoreCase(deployType)) {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("需要部署的是webapp应用,即将开始部署!");
			}
			factory = new WarDeployFactory();
		} else if ("service".equalsIgnoreCase(deployType)) {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("需要部署的是app应用,即将开始部署!");
			}
			factory = new ServiceDeployFactory();
		} else if ("worker".equalsIgnoreCase(deployType)) {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("需要部署的是worker应用,即将开始部署!");
			}
			factory = new WorkerDeployFactory();
		} else {
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error("系统暂不支持类型为: [" + deployType + "]的部署类型,程序退出执行。请检查!");
			}
			System.exit(0);
		}
		// 实例部署类
		Deployer deployer = factory.newInstance();
		// 执行部署方法
		deployer.deploy(root);
	}
}
