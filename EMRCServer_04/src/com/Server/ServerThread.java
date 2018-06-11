package com.Server;

import java.io.*;
import java.net.*;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

// 伺服執行緒
public class ServerThread extends Thread {
	private ServerSocket serverSkt;
	private BroadCastThread _broadCastThread; // 負責廣播

	public ServerThread(int port) {
		setDaemon(true);
		// 啟動廣播執行緒
		_broadCastThread = new BroadCastThread();
		_broadCastThread.start();

		// try {
		// serverSkt = new ServerSocket(port);
		// } catch (IOException e) {
		// print(e.toString());
		// }

		try {
			// 憑證keypass
			char[] clientCerPwd = "123456".toCharArray();
			// 服务端我们采用java默认的密钥库JKS类型，通过KeyStore类的静态方法获得实例并且指定密钥库类型
			KeyStore serverKeyStore = KeyStore.getInstance("JKS");
			// 利用提供的密钥库文件输入流和密码初始化密钥库实例
			serverKeyStore.load(new FileInputStream("./ca/server.keystore"), clientCerPwd);
			// 取得SunX509私钥管理器
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
			// 用之前初始化后的密钥库实例初始化私钥管理器
			keyManagerFactory.init(serverKeyStore, clientCerPwd);
			// 获得TLS协议的SSLContext实例
			SSLContext sslContext = SSLContext.getInstance("TLS");
			// 初始化SSLContext实例
			sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
			// 以下两步获得SSLServerSocket实例
			serverSkt = sslContext.getServerSocketFactory().createServerSocket(port);
			// TCP Socket
			// photoSkt = new ServerSocket(2002);

		} catch (Exception e) {
			print(e.toString());
		}
	}

	public void addSysopMessage(String message) {
		_broadCastThread.out(message);
	}

	public void run() {
		print("SSLSocket thread...");

		while (EMRCServer.ServerRun) {
			Socket clientSkt = null; // 客戶端Socket
			ClientThread client = null; // 客戶端連線

			try {
				clientSkt = serverSkt.accept();
				print(clientSkt.getInetAddress() + " (SSL)Connection...");

				// 啟動一個客戶端執行緒，第二個參數指定廣播執行緒物件
				client = new ClientThread(clientSkt, _broadCastThread);
				client.start();

				// 將客戶端加入廣播執行緒中管理
				_broadCastThread.addClientThread(client);
			} catch (IOException e) {
			}
		}
	}

	public void finalize() {
		try {
			serverSkt.close();
		} catch (IOException e) {
		}
	}

	private static void print(String arg0) {
		System.out.println(arg0);
	}
}