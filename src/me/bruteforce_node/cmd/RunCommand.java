package me.bruteforce_node.cmd;

import java.util.ArrayList;
import java.util.List;
//import me.bruteforce_node.request.Connect2Server;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

public class RunCommand {
	private ArrayList<String> cmd = new ArrayList<String>();
	//private String command;
	//private List<String> params; // = new ArrayList<String>();
	public boolean terminate = false;
	
	private String line_in = ""; //程序执行过程中回显的每一行
	private String line_err = ""; //程序执行过程中回显的每一行
	private File logfile;
	private FileWriter fw;
	
	private InputStream stdin;
//	private InputStream stderr;
	private InputStreamReader isr;
//	private InputStreamReader esr;
	private BufferedReader brin;
//	private BufferedReader brerr;
//	private Runtime rt = Runtime.getRuntime();
	private Process proc;
	
	RunCommand(String command, List<String> params, String log_FileName){
		this.cmd.add(command);
		this.cmd.addAll(params);
//		this.command = command;
//		this.params = params;
//		
//		this.cmd = this.command;
//		for (String p : this.params){
//			this.cmd += " " + p;
//		}
		
		this.logfile = new File(log_FileName);
	}
	
	// 不带回显
	void run(BruteForce_Node node){
		//Runtime rt = Runtime.getRuntime();
		try {
			//this.proc = this.rt.exec(this.cmd);
			//String[] args;
			ProcessBuilder pb = new ProcessBuilder(this.cmd);
			pb = pb.redirectErrorStream(true);
			this.proc = pb.start();
			stdin = proc.getInputStream();
//			stderr = proc.getErrorStream();
			isr = new InputStreamReader(stdin);
//			esr = new InputStreamReader(stderr);
			brin = new BufferedReader(isr);
//			brerr = new BufferedReader(esr);
			
			if (!this.logfile.exists())
				this.logfile.getParentFile().mkdirs();
				this.logfile.createNewFile();
			fw = new FileWriter(this.logfile);
			while (true){
				if (terminate == true){
					proc.destroy();
					fw.write("");
					fw.flush();
					fw.close();
					break;
				}
				//这里用“|”是因为要两边都执行！ //4-6注：用一个竖杠的写法会导致始终阻塞！改用ProcessBuilder
				if ( (this.proc.isAlive())/*这个条件是为了避免之前因为线程太多被destroy过*/ && (this.line_in = brin.readLine()) != null/* || (this.line_err = brerr.readLine()) != null*/){
					if (this.line_in != null){
						fw.append(this.line_in + "\r\n");
						fw.flush();
						System.out.println("Stream   ---->    " + this.line_in);
						node.callback(line_in, true);
						node.callback(line_in, false);
					}
//					if(this.line_err != null){
//						fw.append(this.line_err + "\r\n");
//						fw.flush();
//						System.out.println("ERROR---->    " + this.line_err);
//						node.callback(line_err, false);
//					}
				}
				else {
					break;
				}
			}
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	boolean die(){
		if(this.proc != null){
			this.proc.destroy();
			return true;
		}
		return false;
	}
	
	byte[] readLog(File log) throws IOException{
		if (log.exists()){
			InputStream in = new FileInputStream(log);
			int len = (int)log.length();
			byte[] b = new byte[len];
			in.read(b, 0, len);
			in.close();
			return b;
		}
		else return "".getBytes();
	}
	// 获取当前这一行执行结果回显
	String getCurrentLineIn(){
		return this.line_in;
	}
	
	String getCurrentLineErr(){
		return this.line_err;
	}
}
