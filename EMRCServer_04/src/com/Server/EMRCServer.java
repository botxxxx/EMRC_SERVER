package com.Server;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class EMRCServer {
	private static boolean DBlink = false, Ones = true;
	private static int port = xxxx;
	private static String date = "0611";
	private static Thread DB = null;
	private static ServerThread server = null;
	private static String Host = "127.0.0.1";
	private static String DbSID = "test"; // table
	public static boolean ServerRun = false;
	// ServerRun is used to ServerThread & BroadCastThread
	public static String db = "emrc";
	public static String version = "";
	public static String driver = "org.gjt.mm.mysql.Driver";
	public static String username = "iccl"; // user
	public static String password = "1234"; // pass
	public static String url = "jdbc:mysql://" + Host + ":3306/" + DbSID
			+ "?characterEncoding=utf8&useUnicode=true&useSSL=false&autoReconnect=true";

	public static void main(String[] args) {

		// =============================================================================
		print("EMRCServer....");
		print("最後更新:" + date);
		try {
			// 啟動伺服器執行緒
			String ip = InetAddress.getLocalHost().getHostAddress();
			print("本機IP:" + ip + " " + "連結埠 :" + port);
			new Thread(autoCheck).start();
			DB = new Thread(startLink);
			DB.start();
			DB.join();
			print("=============================");
			// 後台的站台廣播
			setTimer();
			String smsg = null;
			BufferedReader user = new BufferedReader(new InputStreamReader(System.in));
			while ((smsg = user.readLine()) != null && DBlink) {
				if (smsg.length() != 0) {
					if (smsg.substring(0, 1).equals("/")) {
						String sm = smsg.substring(1);
						server.addSysopMessage("SS/" + sm);
						print(sm);
						print("=============================");
					} else {
						print("無效的指令！");
						print("=============================");
					}
				} else {
					server.addSysopMessage(smsg);
					print("out:" + smsg);
				}
			}
			print("END");
		} catch (IOException | InterruptedException e) {
			print(e.toString());
		}
	}

	private static void STOP() {
		DB = null;
	}

	private static void print(Object arg0) {
		System.out.println(arg0);
	}

	public static void getTask() {
		if (!Ones) {
			server.finalize(); // close
			ServerRun = false;
			print("============================");
			Calendar calendar = Calendar.getInstance();
			Date firstTime = calendar.getTime();
			print(firstTime);
		} else {
			Ones = false;
		}
		server = new ServerThread(port);
		server.start(); // start
		ServerRun = true;
	}

	private static Runnable autoCheck = new Runnable() {
		public void run() {
			try {
				Thread.sleep(3000);
			} catch (Exception e) {
				print(e.toString());
			}
			if (!DBlink) {
				STOP();
				print("ERROR DB CAN'T USE!");
			}
		}
	};

	private static Runnable startLink = new Runnable() {
		public void run() {
			try {
				Class.forName(driver);
				Connection conn = DriverManager.getConnection(url, username, password);
				Statement stmt = conn.createStatement();
				String var = "";

				ResultSet rs = stmt.executeQuery("SELECT MAX(number) FROM " + db);
				while (rs.next()) {
					var = rs.getString("MAX(number)");
				}
				if (var.length() > 2) {
					version = var;
					print("更新碼:" + version);
				} else {
					print("更新碼取得錯誤！");
				}

				if (conn != null && !conn.isClosed()) {
					print("資料庫連線測試成功！");
					DBlink = true;
				}
				conn.close();
			} catch (SQLException | ClassNotFoundException e) {
				print(e.toString());
			}
		}
	};

	private static void setTimer() {

		// 設定填入schedule中的 Date firstTime 為現在的10秒後
		// Calendar calendar = Calendar.getInstance();
		// calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 5);
		Date firstTime = Calendar.getInstance().getTime();
		// 設定計時器
		// 第一個參數為"欲執行的工作",會呼叫對應的run() method
		// 第二個參數為程式啟動後,"延遲"指定的毫秒數後"第一次"執行該工作
		// 第三個參數為每間隔多少毫秒執行該工作
		new Timer().schedule(new DateTask(), firstTime, 1000 * 60 * 30);
	}
}

class DateTask extends TimerTask {
	public void run() {
		EMRCServer.getTask();
	}
}