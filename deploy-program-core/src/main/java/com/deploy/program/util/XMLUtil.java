package com.deploy.program.util;

import java.io.File;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.deploy.program.bean.App;
import com.deploy.program.bean.Nginx;
import com.deploy.program.bean.Root;
import com.deploy.program.bean.Tomcat;

public class XMLUtil {
	
	private transient static Logger LOGGER = LoggerFactory.getLogger(XMLUtil.class);

	public static Root readConfigFile(String configFile) {
		if (configFile == null || configFile.isEmpty()) {
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error("解析配置文件传入的配置文件路径为空。");
			}
			return null;
		}
		File file = new File(configFile);
		if (!file.exists()) {
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error("配置文件解析时, 获取不到配置不见路径。");
			}
			return null;
		}
		Root root = new Root();
		SAXReader sax = new SAXReader();
		Document doc = null;
		try {
			doc = sax.read(file);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		Element rootElement = doc.getRootElement();
		String isNew = rootElement.elementText("isNew");
		String type = rootElement.elementText("type");
		Element appElement = rootElement.element("app");
		Element tomcatElement = rootElement.element("tomcat");
		Element nginxElement = rootElement.element("nginx");
		
		root.setIsNew(isNew);
		root.setType(type);
		
		App app = new App();
		// 解析webApp节点的元素
		String appPath = appElement.element("appPath").getText();
		String appName = appElement.element("appName").getText();
		String url = appElement.element("url").getText();
		String port = appElement.element("port").getText();
		String deployPath = appElement.element("deployPath").getText();
		app.setAppPath(appPath);
		app.setAppName(appName);
		app.setUrl(url);
		app.setPort(port);
		app.setDeployPath(deployPath);
		root.setApp(app);
		
		Tomcat tomcat = new Tomcat();
		// 解析tomcat节点的元素
		String catalinaHome = tomcatElement.element("catalinaHome").getText();
	    String catalinaBase = tomcatElement.element("catalinaBase").getText();
	    String executePath = tomcatElement.element("executePath").getText();
	    String serverConfPath = tomcatElement.element("serverConfPath").getText();
	    tomcat.setCatalinaHome(catalinaHome);
	    tomcat.setCatalinaBase(catalinaBase);
	    tomcat.setExecutePath(executePath);
	    tomcat.setServerConfPath(serverConfPath);
	    root.setTomcat(tomcat);
	    
		Nginx nginx = new Nginx();
		// 解析nginx节点的元素
		String nginxExecutePath = nginxElement.element("executePath").getText();
	    String confPath = nginxElement.element("confPath").getText();
		nginx.setExecutePath(nginxExecutePath);
		nginx.setConfPath(confPath);
		root.setNginx(nginx);
		
		return root;
	}
}
