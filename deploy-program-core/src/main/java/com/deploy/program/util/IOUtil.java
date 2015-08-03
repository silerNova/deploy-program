package com.deploy.program.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;

public class IOUtil {
	
	public static boolean copyDirectory(File tarDir, File destDir) {
		boolean flag = false;
		if (!tarDir.exists()) {
			throw new NullPointerException("源目录不存在, 请检查!");
		}
		if (!destDir.exists()) {
			// 创建目标目录
			destDir.mkdir();
		}
		try {
			// 对源目录进行遍历
			File[] files = tarDir.listFiles(new FileFilter(){

				@Override
				public boolean accept(File file) {
					if (file.isFile()) {
						String fileName = file.getName();
						// 过滤掉这两个文件
						if (fileName.equals("host-manager.xml")
								|| fileName.equals("manager.xml")) {
							return false;
						}
					}
					return true;
				}
				
			});
			for (File file : files) {
				File dest = new File(destDir.getAbsolutePath() + File.separator + file.getName());
				if (file.isFile()) {
					// 复制文件内容
					copyFile(file, dest);
				} else {
					// 是目录, 则复制目录
					copyDirectory(file, dest);
				}
			}
			// 复制完成后,置标志位.
			flag = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return flag ;
	}

	public static boolean copyFile(File tarFile, File destFile) {
		boolean  flag = false;
		if (!tarFile.exists()) {
			throw new NullPointerException("源文件不存在, 请检查!");
		}
		if (!destFile.exists()) {
			try {
				destFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// 将源文件中的内容写入目标文件
		InputStream in = null;
		OutputStream out = null;
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		try {
			in = new FileInputStream(tarFile);
			bis = new BufferedInputStream(in);
			out = new FileOutputStream(destFile);
			bos = new BufferedOutputStream(out);
			
			byte[] b = new byte[1024];
			
			int length = 0;
			
			while(true) {
				length = bis.read(b);
				if (length == -1) {
					break;
				} else {
					bos.write(b, 0, length);
				}
			}
			// 将输出缓冲流刷到文件中
			bos.flush();
			flag = true;
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			close(bos);
			close(out);
			close(bis);
			close(in);
		}
		return flag;
	}
	
	public static void unzip(String warPath, String unzipPath) {
	    File warFile = new File(warPath);
	    try {
	        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(warFile));
	        ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.JAR,
	                bufferedInputStream);

	        JarArchiveEntry entry = null;
	        while ((entry = (JarArchiveEntry) in.getNextEntry()) != null) {
	            if (entry.isDirectory()) {
	                new File(unzipPath, entry.getName()).mkdir();
	            } else {
	                OutputStream out = FileUtils.openOutputStream(new File(unzipPath, entry.getName()));
	                IOUtils.copy(in, out);
	                out.close();
	            }
	        }
	        in.close();
	    } catch (FileNotFoundException e) {
	        System.err.println("未找到war文件");
	    } catch (ArchiveException e) {
	        System.err.println("不支持的压缩格式");
	    } catch (IOException e) {
	        System.err.println("文件写入发生错误");
	    }
	}
	
	public static boolean writeNginxConfig(String configFile, String url, String port) {
		boolean flag = false;
		StringBuilder sb = new StringBuilder(100);
		sb.append("upstream " + url + " {\n")
		  .append("server 127.0.0.1")
		  .append(":")
		  .append(port)
		  .append(";\n")
		  .append("}\n");
		sb.append("server {\n")
		  .append("listen 80;\n")
		  .append("server_name ")
		  .append(url)
		  .append(";\n")
		  .append("location / {\n")
		  .append("access_log off;\n")
		  .append("proxy_pass http://" + url + ";\n")
		  .append("}\n")
		  .append("}");
		
		File file = new File(configFile);
		if (!file.exists()) {
			try {
				file.createNewFile();
				// 设置文件权限
				file.setExecutable(true);
				file.setReadable(true);
				file.setWritable(true);
			} catch (IOException e) {
				e.printStackTrace();
			}
			OutputStream out = null;
			BufferedWriter bw = null;
			try {
				out = new FileOutputStream(configFile);
				bw = new BufferedWriter(new OutputStreamWriter(out));
				bw.write(sb.toString());
				bw.flush();
				flag = true;
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				close(bw);
				close(out);
			}
		}
		
		return flag;
	}
	
	public static void close (Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static String getCD(String filePath) {
		// 获取配置路径的盘符
		File tmpFile = new File(filePath);
		String absolutePath = tmpFile.getAbsolutePath();
		// 获取绝对路径下的盘符
		String cd = absolutePath.substring(0, 1);
		return cd;
	}
	
	public static boolean deleteChildrenFile(File parentFile) {
		boolean flag = false;
		if (parentFile == null) {
			throw new NullPointerException("文件对象为空.");
		}
		File[] files = parentFile.listFiles();
		if (files != null && files.length > 0) {
			for (File file : files) {
				if (file.isDirectory()) {
					File[] childrenFiles = file.listFiles();
					if (childrenFiles != null && childrenFiles.length > 0) {
						deleteChildrenFile(file);
					} else {
						file.delete();
					}
				} else if (file.isFile()) {
					file.delete();
				} else {
					throw new RuntimeException("不能识别的文件类型.");
				}
			}
		}
		flag = true;
		return flag;
	}
}
