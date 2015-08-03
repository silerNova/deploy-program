package com.deploy.program.bean;

public class Nginx {

	/*
	 * nginx启动执行路径
	 */
    private String executePath ;
    
    /*
     * nginx配置路径, 是应用的配置路径, 不是nginx.conf的配置路径
     */
    private String confPath ;

	public String getExecutePath() {
		return executePath;
	}

	public void setExecutePath(String executePath) {
		this.executePath = executePath;
	}

	public String getConfPath() {
		return confPath;
	}

	public void setConfPath(String confPath) {
		this.confPath = confPath;
	}
}
