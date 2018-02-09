package me.bruteforce_node.cmd;

import java.io.*;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSONObject;

import me.bruteforce_node.request.Connect2Server;

public class BruteForce_Node {
	private Connect2Server conn2svr;
	private String node_ID;
	private String task_ID;
	private String targets_FileName;
	private String service;
	private String usernames_FileName;
	private String passwords_FileName;
	private String colon = "";
	private String logfile = "BruteForce.log";
	private String resultFile = "result.txt";
	private JSONObject resJSON = new JSONObject();
	private JSONObject statusJSON = new JSONObject();
	private boolean lockStatusFile = false;
	int numTasks = 16;
	int numTasksStart = 0;
	int numTasksEnd = 0;
	private RunCommand runCmd;
	private String Prog = "hydra"; // hydra
	boolean needAjust = true;
	
	//采用用户名字典、密码字典分别为不同文件
	public BruteForce_Node(Connect2Server conn2svr, String task_ID, String targets, String service, String usernames, String passwords) throws IOException{
		this.conn2svr = conn2svr;
		this.Prog = BruteForce_Node.getProp("hydra");
		this.node_ID = conn2svr.getNode_ID();
		this.task_ID = task_ID;
		this.targets_FileName = targets;
		this.service = service;
		this.usernames_FileName = usernames;
		this.passwords_FileName = passwords;
		this.logfile = System.getProperty("user.dir")  + File.separator + "log" + File.separator + this.task_ID + ".log";
		this.resultFile = System.getProperty("user.dir")  + File.separator + "result" + File.separator + this.task_ID + ".txt";
	}
	public BruteForce_Node(Connect2Server conn2svr, String task_ID, String targets, String service, String usernames, String passwords, int num) throws IOException{
		this(conn2svr, task_ID, targets, service, usernames, passwords);
		this.numTasks = num;
	}
	
	//采用“用户名+分隔符+密码”在同一文件colon
	public BruteForce_Node(Connect2Server conn2svr, String task_ID,String targets, String colon) throws IOException{
		this.conn2svr = conn2svr;
		this.Prog = Connect2Server.getProp("hydra");
		this.node_ID = conn2svr.getNode_ID();
		this.task_ID = task_ID;
		this.targets_FileName = targets;
		this.colon = colon;
		this.logfile = System.getProperty("user.dir")  + File.separator + "log" + File.separator + this.task_ID + ".log";
		this.resultFile = System.getProperty("user.dir")  + File.separator + "result" + File.separator + this.task_ID + ".txt";
	}
	public BruteForce_Node(Connect2Server conn2svr, String task_ID,String targets, String colon, int num) throws IOException{
		this(conn2svr, task_ID, targets, colon);
		this.numTasks = num;
	}
	
	private static Properties getProperties() throws FileNotFoundException, IOException{
		Properties prop = new Properties();
		FileInputStream iFile = new FileInputStream(System.getProperty("user.dir") + File.separator + "prog.properties");
		prop.load(iFile);
		iFile.close();
		return prop;
	}
	
	public static String getProp(String key) throws IOException {
		// TODO Auto-generated method stub
		Properties prop = BruteForce_Node.getProperties();
		return prop.getProperty(key);
	}
	
	public static void setProp(String key, String value) throws IOException {
		// TODO Auto-generated method stub
		Properties prop = BruteForce_Node.getProperties();
		prop.setProperty(key,value);
		FileOutputStream oFile = new FileOutputStream(System.getProperty("user.dir") + File.separator + "prog.properties");
		prop.store(oFile, null);
		oFile.flush();
		oFile.close();
	}
	
	public String getTaskID(){
		return this.task_ID;
	}
	
	public String getLog() throws IOException{
		byte[] b;
		File log = new File(this.logfile);
		b = this.runCmd.readLog(log);
		return new String(b);
	}

	public int doBruteForce(){
		List<String> params = new ArrayList<String>();
		params.add("-M"); //目标字典
		params.add(this.targets_FileName);
		if (this.usernames_FileName != null && 
			!this.usernames_FileName.equals("") &&
			this.passwords_FileName != null &&
			!this.passwords_FileName.equals("")){
		
			params.add("-L"); //用户名字典
			params.add(this.usernames_FileName);
			params.add("-P"); //密码字典
			params.add(this.passwords_FileName);
		}else if (this.colon != null && !this.colon.equals("")){
			params.add("-C");
			params.add(this.colon);
		}else {
			System.out.println("账户字典 或 密码字典 或 组合字典 未设置!");
			return -1; //
		}
		params.add("-o"); // 攻击结果
		params.add(this.resultFile);
		params.add("-vV");
		params.add("-t");
		params.add(String.valueOf(this.numTasks));
		params.add(this.service);
		
/*		while (this.needAjust){     // 这里想写监视hydra是不是线程过多，如果是就减少，但是暂时没成功，先注释掉，以后再写
			*/
			this.runCmd = new RunCommand(Prog, params, this.logfile);
/*			monitorRunable monitor = new monitorRunable(this, this.runCmd);
			Thread monitorThread = new Thread(monitor);
			monitorThread.start();*/
			this.runCmd.run(this);
/*			monitor.endWhile = true;
		}*/
//			try {
//				String result = Connect2Server.readFile(this.resultFile);
//				String[] results = result.split("\n");
//				for (int i = 1; i < results.length; i++){
//					JSONObject jsonRes = doTranslateResult(results[i]);
//					doPostResult(jsonRes);
//				}
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		return 0;
	}
	private void doPostResult(JSONObject jsonRes) {
		// TODO Auto-generated method stub
		String data2POST = Connect2Server.JSON2POST(jsonRes);
		try {
			Connect2Server.doPost(Connect2Server.getPostResURL(), data2POST);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			System.out.println("提交结果页面URL错误");
			e.printStackTrace();
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("提交失败!");
			e.printStackTrace();
			return;
		}
		System.out.println("提交结果成功: " + jsonRes.toJSONString());
	}
//	private JSONObject doTranslateResult(String res) {
//		// TODO Auto-generated method stub
//		JSONObject jsonRes = new JSONObject();
//		Pattern pattern = Pattern.compile("\\[([0-9]+)\\]\\[([a-zA-Z]+)\\][\\s]+host:[\\s]+([\\.0-9]+)[\\s]+login:[\\s]+([\\S]+)[\\s]+password:[\\s]+([\\S]*)");
//		Matcher matcher = pattern.matcher("[22][ssh] host: 202.207.177.215   login: test   password: test");
//		System.out.println(matcher.find());
//		
//		jsonRes.put("Port", matcher.group(1));
//		jsonRes.put("Service", matcher.group(2));
//		jsonRes.put("Host", matcher.group(3));
//		jsonRes.put("Username", matcher.group(4));
//		jsonRes.put("Password", matcher.group(5));
//		
//		return jsonRes;
//	}
	public void callback(String line, boolean b) throws IOException {
		// TODO Auto-generated method stub
		if (b){
			// 有结果
			String[] key1 = {Connect2Server.getProp("post_Task_ID"),
							 Connect2Server.getProp("post_Node_ID"),
							 Connect2Server.getProp("post_Port"),
							 Connect2Server.getProp("post_Type"),
							 Connect2Server.getProp("post_Host"),
							 Connect2Server.getProp("post_Login"),
							 Connect2Server.getProp("post_Password")};
			String strRegex1 = "\\[([0-9]+)\\]\\[([a-z]+)\\] host: ([\\.\\d]{7,15})   login: (.+)   password: ([\\s\\S]*)";
			
			// 最终汇总
			/*
			String[] key2 = {Connect2Server.getProp("post_Task_ID"),
					 		 Connect2Server.getProp("post_Node_ID"),
					 		 Connect2Server.getProp("post_Num_Valid")};
			String strRegex2 = "[0-9]+ of [0-9]+ target(?:s successfully) completed, ([0-9]+) valid passwords found";
			*/
			
			// 进度
			String[] key3 = {Connect2Server.getProp("post_Task_ID"),
							 Connect2Server.getProp("post_Node_ID"),
							 Connect2Server.getProp("post_Task_Progress")};
			String strRegex3 = ".+- ([0-9]+) of ([0-9]+) \\[child [0-9]+\\]";
			this.executeWriteStatus(key3, strRegex3, line);
			this.executePost(key1, strRegex1, line);
			//this.executePost(key2, strRegex2, line);  // Modified by Jingyang 20160506 不要最终总计了
		}
		else if (!b){
			if(line.matches("\\[WARNING\\].*use.*-t 4.*")){
				this.runCmd.die();
				this.numTasks -= 2;
				System.out.println("Current number of threads: " + this.numTasks);
				this.doBruteForce();
			}
		}
	}
	private void executeWriteStatus(String[] key, String strRegex, String line) {
		// TODO Auto-generated method stub
		Matcher m = this.extractData(strRegex, line);
		//String[] key = {"port","service","host","login","password"};
		if (m.find()){
			if (this.lockStatusFile == false){
				this.statusJSON.clear();
				this.statusJSON.put(key[0], this.task_ID);
				this.statusJSON.put(key[1], this.node_ID);
				
				this.statusJSON.put(key[2], m.group(1) + "/" + m.group(2));
				
				Connect2Server.writeFile(System.getProperty("user.dir") +File.separator+"status"+File.separator+"status.json", this.statusJSON.toJSONString());
				if (m.group(1).equals(m.group(2))){ // 当 尝试过了最后一条，锁定写入状态文件。（在提交完最后状态之后清空状态，代码在提交状态之后）
					this.lockStatusFile = true;
					System.out.println("最后一条结果");
					this.conn2svr.heartBeats(false);//this.executePost(key, "(.+)", m.group(1)+"/"+m.group(2)); // 提交最后一条进度
				}
			}
		}
	}
	private void executePost(String[] key, String strRegex, String line){
		Matcher m = this.extractData(strRegex, line);
		//String[] key = {"port","service","host","login","password"};
		if (m.find()){
			this.resJSON.clear();
			this.resJSON.put(key[0], this.task_ID);
			this.resJSON.put(key[1], this.node_ID);
			for (int i=1; i <= m.groupCount(); i++){
				this.resJSON.put(key[i+1], m.group(i));
			}
			this.doPostResult(this.resJSON);
		}
	}
	private Matcher extractData(String strRegex, String line){
		//strRegex = "\\[([0-9]+)\\]\\[([a-z]+)\\] host: ([\\.\\d]{7,15})   login: (.+)   password: ([\\s\\S]*)";
		Pattern p = Pattern.compile(strRegex);
		Matcher m = p.matcher(line);
		return m;
	}
}

class monitorRunable implements Runnable{
	private RunCommand runCmd;
	private BruteForce_Node node;
	boolean endWhile = false;
	
	monitorRunable(BruteForce_Node node, RunCommand runCmd) {
		// TODO Auto-generated constructor stub
		this.runCmd = runCmd;
		this.node = node;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
//		boolean normal;
		
		while (!endWhile){
			try {
				String log = this.node.getLog();
				String[] logs;
				logs = log.split("\r\n");
				for(String line : logs){
					if (line.matches("\\[WARNING\\].*use.*-t 4.*")){
						this.runCmd.terminate = true;
						if (node.numTasks != this.seek(node, node.numTasks, false, node.numTasksStart, node.numTasksEnd)){
							node.numTasks = this.seek(node, node.numTasks, false, node.numTasksStart, node.numTasksEnd);
						}
						else {
							endWhile = true;
							node.needAjust = false;
						}
						break;
					}else if (line.matches("\\[DATA\\].*")){
						if (node.numTasks != this.seek(node, node.numTasks, true, node.numTasksStart, node.numTasksEnd)){
							this.runCmd.terminate = true;
							node.numTasks = this.seek(node, node.numTasks, true, node.numTasksStart, node.numTasksEnd);
						}
						else {
							endWhile = true;
							node.needAjust = false;
						}
						break;
					}
				}
				// 休眠1秒后，再次读取log
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("读取运行日志失败!");
				e.printStackTrace();
			}
		}
	}
	
	private int seek(BruteForce_Node node, int t, boolean situation, int start, int end){
		//int start = 0;
		//int end = 0;
		boolean endLock = false;
		//int tmp;
//		int step = -1;
		//while (step !=0)
			if (/*t-7 <= 0*/situation){ // 条件
				start = t;
				if (endLock == false)
					end = 2 * t;
				t = start + (end - start)/2;
//				step = t - start;
			}else{
				end = t;
				endLock = true;
				t = start + Math.floorDiv((end - start), 2);
//				step = end - t;
			}
			node.numTasksStart = start;
			node.numTasksEnd = end;
		return t;
	}
}