package me.bruteforce_node.cmd;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.print.attribute.standard.PDLOverrideSupported;

import org.omg.CosNaming.IstringHelper;


public class RunCmdtest {
	private static Process proc;
	private InputStream is;
	private InputStreamReader isr;
	public static void main(String[] args) {
		ProcessBuilder pb = new ProcessBuilder("java","-jar","BurpUnlimited.jar");
		pb.directory(new File("D:\\\\实验室\\\\Burp1.7.26\\\\BurpUnlimited\\\\"));
		
		try {
			proc = pb.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
	}
}
