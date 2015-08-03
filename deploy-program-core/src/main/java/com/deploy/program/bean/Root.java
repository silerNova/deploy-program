package com.deploy.program.bean;

public class Root {

	/*
	 * 是否是新部署
	 */
	private String isNew ;
	
	/*
	 * 1、 war
	 * 2、service
	 * 3、worker
	 */
	private String type ;
	
	private App app;
	
	private Tomcat tomcat ;
	
	private Nginx nginx ;

	public String getIsNew() {
		return isNew;
	}

	public void setIsNew(String isNew) {
		this.isNew = isNew;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public App getApp() {
		return app;
	}

	public void setApp(App app) {
		this.app = app;
	}

	public Tomcat getTomcat() {
		return tomcat;
	}

	public void setTomcat(Tomcat tomcat) {
		this.tomcat = tomcat;
	}

	public Nginx getNginx() {
		return nginx;
	}

	public void setNginx(Nginx nginx) {
		this.nginx = nginx;
	}
}
