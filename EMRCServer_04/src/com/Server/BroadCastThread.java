package com.Server;

import java.util.Vector;

// 廣播執行緒
public class BroadCastThread extends Thread {
	@SuppressWarnings("rawtypes")
	private Vector _clientVector; // 儲存連線的客戶端
	@SuppressWarnings("rawtypes")
	private Vector _messageVector; // 儲存廣播訊息

	@SuppressWarnings("rawtypes")
	public BroadCastThread() {
		setDaemon(true);
		_clientVector = new Vector();
		_messageVector = new Vector();
	}

	@SuppressWarnings("unchecked")
	public void addClientThread(ClientThread client) {
		// 將客戶端add至處理佇列
		_clientVector.addElement(client);
	}

	public void removeClientThread(ClientThread client) {
		// 移除客戶端
		_clientVector.removeElement(client);
	}

	@SuppressWarnings("unchecked")
	public void out(String message) {
		_messageVector.addElement(message);
	}

	@SuppressWarnings("unchecked")
	public void addMessage(String message) {
		_messageVector.addElement(message);
	}

	public void run() {
		while (EMRCServer.ServerRun) {
			ClientThread client = null;
			String message = null;
			
			// 取出要廣播的訊息
			// 目前沒有訊息就不處理接下來的內容
			if (_messageVector.isEmpty())
				continue;

			message = (String) _messageVector.firstElement();
			_messageVector.removeElement(message);
			// System.out.println(message);

			// 將訊息一個一個丟給客戶端
			for (int i = 0; i < _clientVector.size(); i++) {
				client = (ClientThread) _clientVector.elementAt(i);
				client.System_sendMessage(message);
				// client.System_check();
			}
		}
	}
}