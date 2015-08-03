package com.deploy.program.bean;

public class Tomcat {
	
	/*
	 * tomcat服务器安装路径
	 */
    private String catalinaHome ;
    
    /*
     * tomcat实例路径
     */
    private String catalinaBase ;
    
    /*
     * tomcat实例启动路径
     */
    private String executePath ;
    
    /*
     * tomcat服务器配置路径
     */
    private String serverConfPath ;

	public String getCatalinaHome() {
		return catalinaHome;
	}

	public void setCatalinaHome(String catalinaHome) {
		this.catalinaHome = catalinaHome;
	}

	public String getCatalinaBase() {
		return catalinaBase;
	}

	public void setCatalinaBase(String catalinaBase) {
		this.catalinaBase = catalinaBase;
	}

	public String getExecutePath() {
		return executePath;
	}

	public void setExecutePath(String executePath) {
		this.executePath = executePath;
	}

	public String getServerConfPath() {
		return serverConfPath;
	}

	public void setServerConfPath(String serverConfPath) {
		this.serverConfPath = serverConfPath;
	}
}
