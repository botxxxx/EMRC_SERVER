package com.Server;

import java.util.Vector;

// �s�������
public class BroadCastThread extends Thread {
	@SuppressWarnings("rawtypes")
	private Vector _clientVector; // �x�s�s�u���Ȥ��
	@SuppressWarnings("rawtypes")
	private Vector _messageVector; // �x�s�s���T��

	@SuppressWarnings("rawtypes")
	public BroadCastThread() {
		setDaemon(true);
		_clientVector = new Vector();
		_messageVector = new Vector();
	}

	@SuppressWarnings("unchecked")
	public void addClientThread(ClientThread client) {
		// �N�Ȥ��add�ܳB�z��C
		_clientVector.addElement(client);
	}

	public void removeClientThread(ClientThread client) {
		// �����Ȥ��
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
			
			// ���X�n�s�����T��
			// �ثe�S���T���N���B�z���U�Ӫ����e
			if (_messageVector.isEmpty())
				continue;

			message = (String) _messageVector.firstElement();
			_messageVector.removeElement(message);
			// System.out.println(message);

			// �N�T���@�Ӥ@�ӥᵹ�Ȥ��
			for (int i = 0; i < _clientVector.size(); i++) {
				client = (ClientThread) _clientVector.elementAt(i);
				client.System_sendMessage(message);
				// client.System_check();
			}
		}
	}
}