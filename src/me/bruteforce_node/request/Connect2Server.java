package me.bruteforce_node.request;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import com.alibaba.fastjson.*;

import me.bruteforce_node.cmd.BruteForce_Node;

public class Connect2Server {
	//private String IP_local;
	//private String IP_server;
	//private final int Port_server;
	private String joinURL;
	private String token;
	private final String heartBeatURL;
	private int heartbeatTime = Integer.valueOf(BruteForce_Node.getProp("heartBeatTime"));
	private static String Node_ID;
	private String TargetsURL;
	private String UsersURL;
	private String PwdsURL;
	private static String postResURL = "http://localhost:8080/"; // 仅供调试时临时使用
	
	//private static String Node_Status;
	static boolean exit = false;
	static Scanner scanner = new Scanner(System.in);
	
	////private Socket sock;
	//private OutputStream out;
	//private InputStream in;
	
	// 参数 Join页面的URL、token、心跳页面、目标列表下载页面、用户名字典下载页面、密码字典下载页面
	public Connect2Server(String joinURL, String token, String heartBeatURL, String postResURL) throws MalformedURLException, IOException{
		this.joinURL = joinURL;
		this.token = token;
		Connect2Server.Node_ID = this.join(this.joinURL);
		this.heartBeatURL = heartBeatURL;
		Connect2Server.postResURL = postResURL;
		BruteForce_Node.setProp("node_id", Connect2Server.Node_ID);
		heartBeats(true);
	}
	// 这个构造函数的用途是，之前已经加入集群，意外断开后重新加入
	public Connect2Server(String heartBeatURL, String postResURL) throws FileNotFoundException, IOException{
		Connect2Server.Node_ID = BruteForce_Node.getProp("node_id");
		this.heartBeatURL = heartBeatURL;
		Connect2Server.postResURL = postResURL;
		heartBeats(true);
	}
	public void heartBeats(boolean loop) {
		// TODO Auto-generated method stub
		JSONObject jsonStatus;	// 本结点状态
		String data2POST;		// 要提交的数据
		String strTask;			// 从服务器获取来的任务信息
		JSONObject jsonJob;	// 任务信息转换成JSON
		do{
			try {
				// 这里读取文件，获取状态，拼接data2POST
				jsonStatus = this.getNodeStatus();
				jsonStatus.put(Connect2Server.getProp("post_Node_ID"), Connect2Server.Node_ID);
				data2POST = Connect2Server.JSON2POST(jsonStatus);
				strTask = Connect2Server.doPost(this.heartBeatURL, data2POST); // 发送心跳并接收任务(如果有)
				jsonJob = JSON.parseObject(strTask);
				if(jsonStatus.get(Connect2Server.getProp("post_Task_ID")) == null && jsonJob.get(Connect2Server.getProp("Rcv_Task")) != null){
					JSONObject jsonTask = (JSONObject) jsonJob.get(Connect2Server.getProp("Rcv_Task"));
					String Task_ID = jsonTask.getString(Connect2Server.getProp("Rcv_Task_ID"));
					//File logFile = new File(System.getProperty("user.dir")  + File.separator + "history" + File.separator + Task.getString("Task_ID") + ".txt");
					
					// 读取字符串，转数组，转列表；判断列表里有没有这此的Task_ID
					String historyTasks = Connect2Server.readFile(System.getProperty("user.dir")  + File.separator + "history" + File.separator + "history.txt");
					String[] historyArray = historyTasks.split(",");
					List<String> historyList = Arrays.asList(historyArray);
					if (!historyList.contains(Task_ID)){
						// WORK
						// 此处{"stopTask":[],"task":{"custom":"","node_id":"e5c97aa8-ca1a-49f7-81ad-ce9785585e09","password_url":"http://127.0.0.1:8080/hackthepass1013/download.action?fileName=679fc33d-b6ae-4297-b3ad-05737dba3196.pwds&token=07a4e1f1-e253-4243-bbbd-56e5039cc3d3","progress":"0/0","service_type":"ssh","target_url":"http://127.0.0.1:8080/ControlServer/download.action?task_id=679fc33d-b6ae-4297-b3ad-05737dba3196&fileName=679fc33d-b6ae-4297-b3ad-05737dba3196.targets_1","task_create_time":"2017-10-30 14:39:51.0","task_id":"679fc33d-b6ae-4297-b3ad-05737dba3196","username_url":"http://127.0.0.1:8080/hackthepass1013/download.action?fileName=679fc33d-b6ae-4297-b3ad-05737dba3196.users&token=07a4e1f1-e253-4243-bbbd-56e5039cc3d3"}}
						this.TargetsURL = jsonTask.getString(Connect2Server.getProp("Rcv_TargetsURL"));
						this.UsersURL = jsonTask.getString(Connect2Server.getProp("Rcv_UsersURL"));
						this.PwdsURL = jsonTask.getString(Connect2Server.getProp("Rcv_PwdsURL"));
						Connect2Server.appendHistory(jsonTask.getString(Connect2Server.getProp("Rcv_Task_ID")));
						//Connect2Server.writeNodeStatus(jsonStatus); //写入状态文件//这个千万要注释掉
						WorkHandler workHandler = new WorkHandler(this, jsonTask);
						Thread workThread = new Thread(workHandler);
						workThread.start();
					}
				}
				System.out.println("心跳间歇..." + this.heartbeatTime);
				Thread.sleep(this.heartbeatTime); // 心跳间隔
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				//break;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("与服务器通信失败!");
				e.printStackTrace();
				//break;
			}
		}while(loop);
	}
	private static void appendHistory(String Task_ID) {
		// TODO Auto-generated method stub
		try {
            // 打开一个随机访问文件流，按读写方式
           RandomAccessFile randomFile = new RandomAccessFile(System.getProperty("user.dir")  + File.separator + "history" + File.separator + "history.txt", "rw");
            // 文件长度，字节数
           long fileLength = randomFile.length();
            //将写文件指针移到文件尾。
           randomFile.seek(fileLength);
            randomFile.writeBytes((","+Task_ID));
            randomFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	static void writeNodeStatus(JSONObject jsonStatus) {
		// TODO Auto-generated method stub
		Connect2Server.writeFile(System.getProperty("user.dir")  + File.separator + "status" + File.separator + "status.json", jsonStatus.toJSONString());
	}
	// 加入集群
	private String join(String strURL) throws MalformedURLException, IOException{
		JSONObject json = new JSONObject();
		String strReturn = Connect2Server.doGet(strURL + "?token=" + this.token);
		json = JSONObject.parseObject(strReturn);
		String Node_ID = (String)json.get(Connect2Server.getProp("Join_Rcv_Node_ID"));
		return Node_ID;
	}
	
	//获取配置的方法
	public static String getProp(String key) throws IOException {
		// TODO Auto-generated method stub
		Properties prop = new Properties();
		FileInputStream iFile = new FileInputStream(System.getProperty("user.dir") + File.separator + "taskKeys.properties");
		prop.load(iFile);
		return prop.getProperty(key);
	}
	// GET请求
	static String doGet(String strURL) throws MalformedURLException, IOException{
		// 获得连接
		HttpURLConnection httpUrlConn = httpConnFactory.getHttpConn(strURL, "GET");
		// 设置连接
		httpUrlConn.setConnectTimeout(30000); // 30 seconds timeout
		// 执行连接
		while(true){
			try{
				httpUrlConn.connect();
				break;
			}catch(java.net.SocketTimeoutException timeoutE){
				System.out.println("连接超时,正在重试...");
			}
		}
		// 准备接受返回的内容
		int len = httpUrlConn.getContentLength();
		byte[] b = new byte[len];
		String strReturn;
		// 接受返回的内容
		InputStream in = httpUrlConn.getInputStream();
		in.read(b);
		strReturn = new String(b);
		return strReturn;
	}
	
	// POST请求
	public static String doPost(String strURL, String data2POST) throws MalformedURLException, IOException{
		// 获得连接
		HttpURLConnection httpUrlConn = httpConnFactory.getHttpConn(strURL, "POST", true);
		// 设置连接
		httpUrlConn.setConnectTimeout(10000);
		// 执行连接
		while(true){
			try{
				httpUrlConn.connect();
				break;
			}catch(java.net.SocketTimeoutException timeoutE){
				System.out.println("连接超时,正在重试...");
			}
		}
		// 准备提交数据
		OutputStream out = httpUrlConn.getOutputStream();
		// 开始提交数据
		out.write((data2POST).getBytes());
		//out.write("\r\n".getBytes()); // 不可以带这个，否则会破坏提交的数据，方法里其实自动加了换行来分隔内容
		out.flush();
		out.close();
		// 准备接受返回的内容
		int len = httpUrlConn.getContentLength();
		byte[] b = new byte[len];
		String strReturn;
		// 接受返回的内容
		InputStream in = httpUrlConn.getInputStream();
		in.read(b);
		in.close();
		strReturn = new String(b);
		return strReturn;
	}

	// GET下载
	static void downloadGET(String strURL, String filePathName) throws MalformedURLException, IOException{
		// 获得连接
		HttpURLConnection httpUrlConn = httpConnFactory.getHttpConn(strURL, "GET");
		// 设置连接
		httpUrlConn.setConnectTimeout(30000); // 30 seconds timeout
		// 执行连接
		while(true){
			try{
				httpUrlConn.connect();
				break;
			}catch(java.net.SocketTimeoutException timeoutE){
				System.out.println("连接超时,正在重试...");
			}
		}
		// 准备接受返回的内容,准备文件
		int length = 1024;
		int lenReal;
		byte[] b = new byte[length];
		File f = new File(filePathName);
		if (!f.exists()){
			f.getParentFile().mkdirs();
			f.createNewFile();
		}
		OutputStream out = new FileOutputStream(f);
		
		// 接受返回的内容
		InputStream in = httpUrlConn.getInputStream();
		
		// 写入文件
		while (-1 != (lenReal = in.read(b, 0, length)))
			out.write(b, 0, lenReal);
		
		out.close();
		in.close();
	}
	// 读文件
	public static String readFile(String location) throws IOException{
		File fStatus = new File(location);
		String str = null;
		try{
			InputStream fStatusIn = new FileInputStream(fStatus);
			//无阻塞读取字节的个数
			int len = fStatusIn.available();
			byte[] b = new byte[len];
			fStatusIn.read(b);
			str = new String(b,0,len);
			fStatusIn.close();
		} catch(FileNotFoundException e){
			System.out.println("未找到文件,正在创建" + location);
			
			fStatus.getParentFile().mkdirs(); // 创建父目录
			if(fStatus.createNewFile() == true)
				System.out.println("创建成功.");
			else{
				System.out.println("文件创建失败!");
				throw e;
			}
		}
		return str;
	}
	
	/** 

     * 随机读取文件内容 

     */ 

    public static String readFileByRandomAccess(String fileName) { 
    	String line = "";
        RandomAccessFile randomFile = null; 
        try { 
            //System.out.println("随机读取一段文件内容："); 
            // 打开一个随机访问文件流，按只读方式 
            randomFile = new RandomAccessFile(fileName, "r"); 
            // 文件长度，字节数 
            //long fileLength = randomFile.length(); 
            // 读文件的起始位置 
            int beginIndex = 0; 
            // 将读文件的开始位置移到beginIndex位置。 
            randomFile.seek(beginIndex); 
            byte[] bytes = new byte[16]; 
            int byteread = 0; 
            // 一次读10个字节，如果文件内容不足10个字节，则读剩下的字节。 
            // 将一次读取的字节数赋给byteread 
            while ((byteread = randomFile.read(bytes)) != -1) { 
                line += new String(bytes, 0, byteread); 
            } 
        } catch (IOException e) { 
            e.printStackTrace(); 
        } finally { 
            if (randomFile != null) { 
                try { 
                    randomFile.close(); 
                } catch (IOException e1) { 
                } 
            } 
        } 
        return line;
    }
	
	// 写文件
	public static boolean writeFile(String location, String text){
		File f = new File(location);
		boolean result = false;
		while(true){
			try {
				OutputStream out = new FileOutputStream(f);
				out.write((text).getBytes());
				out.close();
				result = true;
				break;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				try {
					f.getParentFile().mkdirs();
					f.createNewFile();
				} catch (Exception IOE) {
					// TODO Auto-generated catch block
					//e1.printStackTrace();
					System.out.println("文件创建失败!");
					return result;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.print("文件写入失败!");
				return result;
			}
		}
		return result;
	}
	
	// 获取结点状态
	private JSONObject getNodeStatus() throws FileNotFoundException, IOException{
		String strJSONStatus = Connect2Server.readFileByRandomAccess(System.getProperty("user.dir")  + File.separator + "status" + File.separator + "status.json");
		JSONObject JSONStatus = JSONObject.parseObject(strJSONStatus);
		if (JSONStatus == null){
			JSONStatus = new JSONObject(); 
		}
		return JSONStatus;
//		String nodeStatus = (String)JSONStatus.get("Node_Status");
//		return nodeStatus;
	}
	
	// JSON转post提交的数据
	public static String JSON2POST(JSONObject JSONStatus){
		String data2POST = JSONStatus.toJSONString().replaceAll("[{}\"]", "");
		data2POST = data2POST.replace(':', '=');
		return data2POST = data2POST.replace(',', '&');
	}
	
	public static String getPostResURL(){
		return Connect2Server.postResURL;
	}
	public String getTargetsURL(){
		return this.TargetsURL;
	}
	public String getUsersURL(){
		return this.UsersURL;
	}
	public String getPwdsURL(){
		return this.PwdsURL;
	}
///////////////////////////////////////////////////////////
	public String getNode_ID() {
		// TODO Auto-generated method stub
		return Connect2Server.Node_ID;
	}
	
//	public void connect() throws IOException{
//	  /**************连到服务器先打招呼***************/
//		OutputStream out;
//		out = this.sock.getOutputStream();
//		//in = this.sock.getInputStream();
//		out.write(token.getBytes());
//		out.flush();
//		//out.close();
//		
//	  /**************启动线程(发送消息)***************/
//		//发送消息在线程
//		HandlerSend handlersend = new HandlerSend(this.sock);
//		Thread threadSend = new Thread(handlersend);
//		threadSend.start();
//		
//	  /**************启动线程(接受消息)***************/
//		//接受消息的线程
//		HandlerRecieve handlerRcv = new HandlerRecieve(sock);
//		Thread threadRcv = new Thread(handlerRcv);
//		threadRcv.start();
//		
//	}
}

// 连接工厂 类
class httpConnFactory{
	static HttpURLConnection getHttpConn(String strURL, String Method) throws MalformedURLException, IOException{
		URL url = new URL(strURL);
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		httpConn.setRequestMethod(Method);
		return httpConn;
	}
	static HttpURLConnection getHttpConn(String strURL, String Method, boolean enableOutput) throws MalformedURLException, IOException{
		HttpURLConnection httpConn = httpConnFactory.getHttpConn(strURL, Method);
		httpConn.setDoOutput(enableOutput);
		return httpConn;
	}
}


/*************发送消息的Runnable**************/
/*
 * 这是个废弃的方法
 * 
 * */
class HandlerSend implements Runnable{
	//String msg = null;
	private Socket sock;
	
	HandlerSend(Socket sock){
		this.sock = sock;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			OutputStream out = this.sock.getOutputStream();
		  /**********将来实际上是发送状态和结果，也要用死循环**********/
			// 死循环，一直等输入
			while (true){
				String line = Connect2Server.scanner.nextLine();
				// 如果退出标志 为 true 并且 输入q ，此时关闭资源，跳出循环，线程结束
				if (Connect2Server.exit && line.equals("q")){ 
					Connect2Server.scanner.close();
					out.close();
					break;
				}
				// 不然，也就是说从console接收到了输入，那么把接受到的输入的内容发送出去给服务器
				line += "\r\n";
				out.write(line.getBytes());
				Thread.sleep(1000);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

/********************接收消息的Runnable********************88*/
class HandlerRecieve implements Runnable{
	private Socket sock;
	public HandlerRecieve(Socket sock) {
		// TODO Auto-generated constructor stub
		this.sock = sock;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		InputStream in;
		try {
			in = this.sock.getInputStream();
			InputStreamReader inr = new InputStreamReader(in);
			BufferedReader bfrin = new BufferedReader(inr);
			String strRcv;
			while (!Connect2Server.exit){
				try {
					// 主要代码在这里
					strRcv = bfrin.readLine();
					System.out.println(strRcv);
				} catch (SocketException e) {
					// TODO Auto-generated catch block
					System.out.println("服务器断开了...输入q退出.");
					Connect2Server.exit = true;
					try {
						this.sock.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}
}

///////////////////////////////////////////////////////////////////////


class WorkHandler implements Runnable{
	private Connect2Server conn2svr;
	private String Task_ID;
	private String targetsFile;
	private String service;
	private String usersFile;
	private String pwdsFile;
	public WorkHandler(Connect2Server conn2svr,JSONObject Task) throws IOException {
		// TODO Auto-generated constructor stub
		this.conn2svr = conn2svr;
		this.Task_ID = Task.getString(Connect2Server.getProp("Rcv_Task_ID"));
		// 目标列表字典
		this.targetsFile = System.getProperty("user.dir")  + File.separator + "dicts" + File.separator + this.Task_ID + ".targets";
		this.service = Task.getString(Connect2Server.getProp("Rcv_Service_Type"));
		this.usersFile = System.getProperty("user.dir")  + File.separator + "dicts" + File.separator + this.Task_ID + ".users";
		this.pwdsFile = System.getProperty("user.dir")  + File.separator + "dicts" + File.separator + this.Task_ID + ".pwds";
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
//		String queryStr = "Node_ID=" + conn2svr.getNode_ID() + "&Task_ID=" + this.Task_ID;
		
		// 获取目标字典、用户字典、密码字典
		try {
			System.out.println("正在下载目标列表...");
			Connect2Server.downloadGET(conn2svr.getTargetsURL()/* + "?" + queryStr*/, this.targetsFile);
			System.out.println("正在下载用户名字典...");
			Connect2Server.downloadGET(conn2svr.getUsersURL()/* + "?" + queryStr*/, this.usersFile);
			System.out.println("正在下载密码字典...");
			Connect2Server.downloadGET(conn2svr.getPwdsURL()/* + "?" + queryStr*/, this.pwdsFile);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			System.out.println("服务器地址格式不正确");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("访问服务器失败");
			e.printStackTrace();
		}
		BruteForce_Node node;
		try {
			node = new BruteForce_Node(conn2svr, this.Task_ID, this.targetsFile, this.service, this.usersFile, this.pwdsFile);
			node.doBruteForce();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			conn2svr.heartBeats(false);
			Connect2Server.writeNodeStatus(new JSONObject()); // 清空状态。
		}
	}
}