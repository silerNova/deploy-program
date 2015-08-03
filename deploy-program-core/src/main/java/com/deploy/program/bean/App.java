package com.deploy.program.bean;

public class App {
    
	/*
	 * app包存放路径
	 */
    private String appPath ;
    
    /*
     * app包的名称
     */
    private String appName ;
    
    /*
     * web访问路径
     */
    private String url ;
    
    /*
     * web访问端口
     */
    private String port ;
    
    /*
     * 部署路径
     */
    private String deployPath ;

	

	public String getAppPath() {
		return appPath;
	}

	public void setAppPath(String appPath) {
		this.appPath = appPath;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getDeployPath() {
		return deployPath;
	}

	public void setDeployPath(String deployPath) {
		this.deployPath = deployPath;
	}
}
