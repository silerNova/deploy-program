package com.deploy.program.deploy.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.deploy.program.bean.App;
import com.deploy.program.bean.Nginx;
import com.deploy.program.bean.Root;
import com.deploy.program.bean.Tomcat;
import com.deploy.program.deploy.interfaces.Deployer;
import com.deploy.program.util.IOUtil;
import com.deploy.program.util.TomcatUtil;

public class WarDeployer implements Deployer {
	
	private transient static final Logger log = LoggerFactory.getLogger(WarDeployer.class);

	@Override
	public boolean deploy(Root root) {
		boolean retFlag = false;
		// 根据配置文件内容进行操作
		// 判断是新建的部署任务还是重部署
		String isNewStr = root.getIsNew();
		boolean isNew = isNewStr != null && "true".equalsIgnoreCase(isNewStr) ? true : false;
		if (isNew) {
			if (log.isInfoEnabled()) {
				log.info("此部署任务为新部署任务");
			}
			/*
			 * 1、将应用部署到对应路径中
			 */
			// 获取应用访问 url和端口
			App webApp = root.getApp();
			String url = webApp.getUrl();
			String port = webApp.getPort();
			if (null == url || url.isEmpty()) {
				if (log.isErrorEnabled()) {
					log.error("url为空, 请检查!");
				}
				throw new NullPointerException("url为空, 请检查!");
			}
			// 获取应用的唯一标识路径
			String webLocal = url.replace(url.substring((url.lastIndexOf(".") + 1)), "local");
			// 将war包解压到部署目录
			String warPath = webApp.getAppPath();
			String warName = webApp.getAppName();
			String deployPath = webApp.getDeployPath() + File.separator + webLocal;
			IOUtil.unzip(warPath + File.separator + warName, deployPath);
			if (log.isInfoEnabled()) {
				log.info(warName + ",已经解压部署到部署路径: " + deployPath);
			}
			// ==============================   web war解压完成    ====================================//
			/* 
			 * 2、 创建tomcat实例的路径
			 */
			if (log.isInfoEnabled()) {
				log.info("开始创建tomcat服务实例。");
			}
			// 获得tomcat实例部署路径
			Tomcat tomcat = root.getTomcat();
			String instancePath = tomcat.getCatalinaBase();
			// 获取应用的tomcat的实例路径
			instancePath = instancePath + File.separator + webLocal;
			if (log.isInfoEnabled()) {
				log.info("tomcat服务实例部署路径为: " + instancePath);
			}
			// 创建目录
			File instanceFile = new File(instancePath);
			if (instanceFile.exists()) {
				if (log.isErrorEnabled()) {
					log.error("tomcat实例目录已经存在,程序退出。请检查!");
				}
		        System.exit(0);
			} else {
				instanceFile.mkdirs();
			}
			// 复制tomcat服务器中的配置文件.
			// 获取tomcat服务器配置文件路径
			String serverConfPath = tomcat.getServerConfPath();
			File serverConfFile = new File(serverConfPath);
			String configDirectoryName = serverConfFile.getName();
			// 新建实例的配置目录
			String instanceConfPath = instancePath + File.separator + configDirectoryName;
			// 将tomcat服务器上的配置目录复制到实例配置目录中,并过滤掉conf/Catalina/localhost中的文件
			boolean flag = IOUtil.copyDirectory(serverConfFile, new File(instanceConfPath));
			if (flag) {
				if (log.isInfoEnabled()) {
					log.info("tomcat服务实例复制tomcat服务器配置文档成功!");
				}
			}
			// 创建tomcat实例的其他目录bin、logs、work、temp
			TomcatUtil.createFiles(tomcat.getCatalinaHome(), instancePath);
			// 创建完成, 修改实例conf目录中的server.xml中的端口号
			flag = TomcatUtil.changePort(new File(instanceConfPath + File.separator + "server.xml"), port);
			if (flag) {
				if (log.isInfoEnabled()) {
					log.info("tomcat服务实例修改应用访问端口成功, 修改端口号为: " + port);
				}
			}
			// 在实例中配置ROOT.xml文件, 关联应用
			String rootConfigFile = instanceConfPath + File.separator + 
					"Catalina" + File.separator + "localhost" + File.separator + 
					"ROOT.xml";
			TomcatUtil.writeRootConf(rootConfigFile, deployPath);
			// 发命令启动tomcat实例, 因为是第一次部署, 所以实例肯定没有启动, 没有必要判断实例是否启动
			String instanceStartCmd = null;
			String systemName = System.getProperty("os.name");
			if (systemName.startsWith("Windows")) {
				// 如果是windows系统, 则需要做以下的事情: 
				// 1、获取配置路径的盘符
				// String cd = IOUtil.getCD(tomcat.getCatalinaBase());
				// 2、执行命令
				// instanceStartCmd = "cmd /c start " + cd + ": && start " + tomcat.getCatalinaBase() + 
						// File.separator + webLocal + File.separator + "bin" + File.separator + "start.bat start";
				instanceStartCmd = "cmd /c " + tomcat.getCatalinaBase() + 
						File.separator + webLocal + File.separator + "bin" + File.separator + "start.bat start";
				Runtime instanceRun = Runtime.getRuntime();
				try {
					instanceRun.exec(instanceStartCmd);
					if (log.isInfoEnabled()) {
						log.info("启动tomcat服务实例!");
					}
				} catch (Exception e1) {
					if (log.isErrorEnabled()) {
						log.error("启动tomcat服务实例失败, 请检查!");
					}
					e1.printStackTrace();
				}
			} else {
				String[] linuxCmds = {"/bin/sh", "-c", tomcat.getCatalinaBase() + 
						File.separator + webLocal + File.separator + "bin" + File.separator + "start.sh"};
				Runtime instanceRun = Runtime.getRuntime();
				try {
					instanceRun.exec(linuxCmds);
					if (log.isInfoEnabled()) {
						log.info("启动tomcat服务实例!");
					}
				} catch (Exception e) {
					if (log.isErrorEnabled()) {
						log.error("启动tomcat服务实例失败, 请检查!");
					}
					e.printStackTrace();
				}
			}
			// ==============================   tomcat配置完成       ====================================//
			/*
			 * 3、配置nginx反向代理
			 */
			if (log.isInfoEnabled()) {
				log.info("开始配置nginx反向代理配置。");
			}
			// 修改Nginx的配置文件, 关联tomcat实例
			Nginx nginx = root.getNginx();
			String nginxConfPath = nginx.getConfPath();
			String nginxConfFile = webLocal + ".conf";
			// 写配置文件内容到配置目录中
		    flag = IOUtil.writeNginxConfig(nginxConfPath + File.separator + nginxConfFile, url, port);
		    if (flag) {
		    	if (log.isInfoEnabled()) {
		    		log.info("写nginx配置文件成功, 成功写入配置文件路径: " + nginxConfPath + File.separator + nginxConfFile);
		    	}
		    }
			// 发命令, 重新加载nginx配置
			String nginxBinPath = nginx.getExecutePath();
			String nginxReStartCmd = null;
			// 下面的代码写的够low的.看不下去了.凑合凑合吧
			if (systemName.startsWith("Windows")) {
				// 1、获取盘符
				String cd = IOUtil.getCD(nginxBinPath);
				// 2、判断是否存在nginx实例, 判断nginx是否已经启动可以判断PID文件是否存在
				/* String checkProcess = "cmd /c wmic process get name";
				Runtime runtime = Runtime.getRuntime();
				try {
					Process process = runtime.exec(checkProcess);
				} catch (IOException e) {
					e.printStackTrace();
				} */
				// 3、直接重启, 这里可以启专门的线程来控制先停后启
				nginxReStartCmd = "cmd /c " + cd + ": && cd " + nginxBinPath + " && nginx.exe -s quit";
				Runtime run = Runtime.getRuntime();
				try {
					run.exec(nginxReStartCmd);
					nginxReStartCmd = "cmd /c " + cd + ": && cd " + nginxBinPath + " && start nginx";
					// 休眠10秒钟
					Thread.sleep(1000 * 10);
					run.exec(nginxReStartCmd);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				nginxReStartCmd = nginxBinPath + File.separator + "nginx -s stop";
				Runtime run = Runtime.getRuntime();
				BufferedReader reader = null;
				try {
					Process proc = run.exec(nginxReStartCmd);
					if (log.isInfoEnabled()) {
						log.info("正在关闭nginx服务器。");
					}
					proc.waitFor();
					nginxReStartCmd = nginxBinPath + File.separator + "nginx";
					// 启动
					proc = run.exec(nginxReStartCmd);
					if (log.isInfoEnabled()) {
						log.info("正在启动nginx服务器。");
					}
					reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
					String line = null;
					while(true) {
						line = reader.readLine();
						if (null == line) {
							break;
						}
						if (log.isErrorEnabled()) {
							log.error("启动nginx时得到的信息为: " + line);
						}
					}
				} catch (Exception e) {
					if (log.isErrorEnabled()) {
						log.error("重启nginx服务器失败, 请检查。");
					}
					e.printStackTrace();
				} finally {
					IOUtil.close(reader);
				}
			}
			// =============================   nginx反向代理配置完成       ====================================//
		} else {
			// 创建重新部署任务: 停tomcat实例服务, 将部署路径的应用删除, 重新部署应用, 启动tomcat实例服务
			Tomcat tomcat = root.getTomcat();
			App webApp = root.getApp();
			String url = webApp.getUrl();
			if (null == url || url.isEmpty()) {
				if (log.isErrorEnabled()) {
					log.error("url为空, 请检查!");
				}
				throw new NullPointerException("url为空, 请检查!");
			}
			// 获取应用的唯一标识路径
			String webLocal = url.replace(url.substring((url.lastIndexOf(".") + 1)), "local");
			// 1、停止tomcat服务实例
			String catalinaBase = tomcat.getCatalinaBase();
			String instanceCmd = null;
			String systemName = System.getProperty("os.name");
			if (systemName.startsWith("Windows")) {
				// 如果是windows系统
				// 获取盘符
				String cd = IOUtil.getCD(catalinaBase);
			    instanceCmd = "cmd /c " + cd + ": && start " + catalinaBase + 
						File.separator + webLocal + File.separator + "bin" + File.separator + "stop.bat start";
			    Runtime instanceStop = Runtime.getRuntime();
				try {
					instanceStop.exec(instanceCmd);
					if (log.isInfoEnabled()) {
						log.info("停止tomcat服务实例!");
					}
				} catch (Exception e) {
					if (log.isErrorEnabled()) {
						log.error("停止tomcat服务实例失败, 请检查!");
					}
					e.printStackTrace();
				}
			} else {
				String[] linuxCmds = {"/bin/sh", "-c", catalinaBase + File.separator + webLocal + File.separator + "bin" + File.separator + "stop.sh"}; 
				Runtime instanceStop = Runtime.getRuntime();
				try {
					instanceStop.exec(linuxCmds);
					if (log.isInfoEnabled()) {
						log.info("停止tomcat服务实例!");
					}
				} catch (Exception e) {
					if (log.isErrorEnabled()) {
						log.error("停止tomcat服务实例失败, 请检查!");
					}
					e.printStackTrace();
				}
			}
			// 2、将部署目录中的老的应用删除, 将新的部署进去
			// 将war包解压到部署目录
			String warPath = webApp.getAppPath();
			String warName = webApp.getAppName();
			String deployPath = webApp.getDeployPath() + File.separator + webLocal;
			// 删除部署目录下的旧应用
			File file = new File(deployPath);
			if (!file.exists() || file.isFile()) {
				if (log.isErrorEnabled()) {
					log.error("部署目录不存在或是一个文件.");
				}
				file.mkdirs();
			}
			// 进行删除
			boolean flag = IOUtil.deleteChildrenFile(file);
			if (flag) {
				if (log.isInfoEnabled()) {
					log.info("成功删除旧应用.");
				}
			}
			// 将新应用部署到目录中
			IOUtil.unzip(warPath + File.separator + warName, deployPath);
			if (log.isInfoEnabled()) {
				log.info(warName + ",已经解压部署到部署路径: " + deployPath);
			}
			// 3、启动tomcat服务实例
			if (systemName.startsWith("Windows")) {
				// 如果是windows系统
				// 获取盘符
				// String cd = IOUtil.getCD(catalinaBase);
				instanceCmd = "cmd /c start " + catalinaBase + 
						File.separator + webLocal + File.separator + "bin" + File.separator + "start.bat start";
			} else {
				instanceCmd = "/bin/sh -c " + catalinaBase + 
					    File.separator + webLocal + File.separator + "bin" + File.separator + "start.sh";
			}
			
			Runtime instanceRun = Runtime.getRuntime();
			try {
				instanceRun.exec(instanceCmd);
				if (log.isInfoEnabled()) {
					log.info("启动tomcat服务实例!");
				}
			} catch (Exception e1) {
				if (log.isErrorEnabled()) {
					log.error("启动tomcat服务实例失败, 请检查!");
				}
				e1.printStackTrace();
			}
			if (systemName.startsWith("Windows")) {
				// 将cmd窗口关闭
				Runtime rt = Runtime.getRuntime();
				try {
				   rt.exec("cmd.exe /C start wmic process where name='cmd.exe' call terminate");
				} catch (Exception e) {
				   e.printStackTrace();
				}
			}
		}
		if (log.isInfoEnabled()) {
			log.info("程序成功完成应用部署!");
		}
		retFlag = true;
		return retFlag;
	}

}
