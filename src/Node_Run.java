import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;

import me.bruteforce_node.cmd.BruteForce_Node;
import me.bruteforce_node.request.Connect2Server;

public class Node_Run {

	public static void main(String[] args) throws IOException, ConnectException, UnknownHostException{
		// TODO Auto-generated method stub
		String joinURL;
		String token;
		String heartBeatURL;
		String postResURL;
		// 判断是否用法正确
		if (args.length != 0){
			System.out.println("\r\n/************ Usage: ************/\r\n");
			System.out.println("0、首先确保该jar文件同目录下有:");
			System.out.println("    prog.properties、taskKeys.properties文件;\r\n    /history/history.txt文件;\r\n    /status/status.json文件;\r\n    dicts、history、log、result、status文件夹");
			System.out.println("1、先编辑prog.properties, 设置 hydra joinURL token heartBeatURL postResURL。");
			System.out.println("2、工作目录进入该jar文件目录, 执行: java -jar bruteforce_node.jar");
			System.out.println("3、其他情况均显示此用法信息\r\n");
			System.out.println("/********************************/");
			return;
		}
		heartBeatURL = BruteForce_Node.getProp("heartBeatURL");//args[2]; // 两种运行方式都需要这个变量，所以提出来
		postResURL = BruteForce_Node.getProp("postResURL");//args[3];
		if (BruteForce_Node.getProp("node_id") == null || BruteForce_Node.getProp("node_id").equals("")){
			joinURL = BruteForce_Node.getProp("joinURL");//args[0];
			token = BruteForce_Node.getProp("token");//args[1];
			new Connect2Server(joinURL, token, heartBeatURL, postResURL);
		}else{ // 意外断开，重新连接
			new Connect2Server(heartBeatURL, postResURL);
		}
	}
}
