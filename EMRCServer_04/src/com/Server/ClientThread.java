package com.Server;

import java.net.*;
import java.nio.file.*;
import java.sql.*;
import java.io.*;
import java.util.*;

// Client執行緒
public class ClientThread extends Thread {
	private boolean Clear = true, Car = false, New = false, sktLink = false;
	private static int _clientNum = 0; // 客戶端連線數
	private String nickName = "Guest", file, brand = "";
	private String[] car_status = { "出勤", "到場", "離場", "到院", "離院", "待命", "取消", "拒絕", "離線" };
	private Socket _skt;
	private BroadCastThread _broadCastThread; // 廣播執行緒
	private Statement stmt;

	public ClientThread(Socket skt, BroadCastThread broad) {
		setDaemon(true);
		_skt = skt;
		_broadCastThread = broad;
		_clientNum++;
	}

	public void System_sendMessage(String message) {
		try {
			// print(message);
			PrintWriter out = new PrintWriter(
					new BufferedWriter(new OutputStreamWriter(new DataOutputStream(_skt.getOutputStream()), "UTF-8")),
					true);
			out.println(message);
			out.flush();
		} catch (IOException e) {
			print(e.toString());
		}
	}

	public void System_check() {
		sktLink = false;
		new Thread(autoCheck).start();
	}

	public void run() {

		try {
			Class.forName(EMRCServer.driver);
			Connection conn = DriverManager.getConnection(EMRCServer.url, EMRCServer.username, EMRCServer.password);
			stmt = conn.createStatement();
			String msg;
			print("目前有　" + _clientNum + "　人在線上......");
			// 建立空的檔案空間
			BufferedReader buf = new BufferedReader(new InputStreamReader(_skt.getInputStream(), "UTF-8"));
			while ((msg = buf.readLine()) != null) {
				// print(msg);
				if (msg.length() < 3) {
					switch (msg) {
					case "a0":
						// 取得流水號
						if (nickName.length() > 9 && Clear) {
							Delete();
						}
						Clear = true;
						String numbers = get_NO();
						System_sendMessage("NO/" + numbers + "|");
						nickName = numbers;
						print(nickName + " > a0");
						break;
					case "a1":
						// 客戶端交握
						// print(nickName + " > a1");
						System_sendMessage("a1");
						break;
					case "b0":
						// 照片
						Clear = false;
						print(nickName + " > b0");
						setPhoto_toDB(DateInput(), 0);
						break;
					case "b1":
						// 照片
						Clear = false;
						print(nickName + " > b1");
						setPhoto_toDB(DateInput(), 1);
						break;
					case "b2":
						// 照片
						Clear = false;
						print(nickName + " > b2");
						setPhoto_toDB(DateInput(), 2);
						break;
					case "b3":
						// 照片
						Clear = false;
						print(nickName + " > b3");
						setPhoto_toDB(DateInput(), 3);
						break;
					case "b4":
						// 照片
						Clear = false;
						print(nickName + " > b4");
						setPhoto_toDB(DateInput(), 4);
						break;
					case "b5":
						// 照片
						Clear = false;
						print(nickName + " > b5");
						setPhoto_toDB(DateInput(), 5);
						break;
					case "b6":
						// 照片
						Clear = false;
						print(nickName + " > b6");
						setPhoto_toDB(DateInput(), 6);
						break;
					case "c0":
						// 更新車輛
						print(nickName + " > c0");
						if (setlectCAR_AL().length() > 1) {
							// System_sendMessage("NC/" + setlectCAR_AL() +
							// '|');
							_broadCastThread.addMessage("NC/" + setlectCAR_AL() + "|");
						}
						break;
					case "c1":
						// 首次更新
						print(nickName + " > c1");
						if (setlectCAR_AL().length() > 1) {
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							System_sendMessage("NC/" + setlectCAR_AL() + '|');
						}
						break;
					case "c2":
						// 更新傷患資訊
						print(nickName + " > c2");
						getRYGB_toDB();
						getNFC_toDB();
						break;
					case "c3":
						// 更新傷患資訊
						print(nickName + " > c3");
						getHospital();
						break;
					case "s1":
						sktLink = true;
						// print(nickName + " > s1");
						break;
					default:
						print(getTimer() + " > " + msg);
						break;
					}
				} else {
					boolean n = true;
					String tg = msg.substring(0, 3);
					if (tg.equals("NFC") || tg.equals("RUN") || tg.equals("GPS") || tg.equals("CAR")
							|| tg.equals("SMS")) {
						n = false;
					}
					if (msg.length() == 11 && n) {
						// 讀取手環時
						if (nickName.length() > 9) {
							Delete();
						}
						Clear = false;
						nickName = msg;
						print(msg + " > getPhoto_fromDB");
						System_sendMessage("ok");
						for (int i = 6; i >= 0; i--) {
							String BIT = setlectBitmaps(i);
							if (BIT != null && BIT.length() > 4) {
								file = BIT;
								Path p = Paths.get(file);
								if (Files.exists(p)) {
									System_sendMessage("b" + i);
									Thread SD = new Thread(sendData);
									SD.start();
									try {
										SD.join();
										Thread.sleep(1000);
									} catch (InterruptedException e) {
										print(e.toString());
									}
								}
							}
						}
						System_sendMessage("b7");
					} else {
						String tag = msg.substring(0, 3), mmsg = msg;
						switch (tag) {
						case "NFC": // NFC save
							Clear = false;
							print(nickName + " > NFC");
							setNFC_toDB(mmsg);
							print(mmsg);
							break;
						case "RUN": // Car status
							Clear = false;
							print(nickName + " > RUN");
							setRUN_toDB(mmsg);
							break;
						case "GPS": // GPS location
							Clear = false;
							print(nickName + " > GPS");
							setGPS_toDB(mmsg);
							break;
						case "CAR": // Car update
							setCAR_toDB(mmsg);
							break;
						case "SMS": // Car synchronized
							setMSG_toCAR();
							break;
						default:
							print(nickName + " > " + msg);
							// System_sendMessage(msg);
							break;
						}
					}
				}
			}
		} catch (IOException | ClassNotFoundException | SQLException e) {
			print(e.toString());
		} finally {// 連線終止
			removeClienthread();
		}
	}

	private void removeClienthread() {
		try {
			_skt.close();
			if (Clear) {
				Delete();
			}
			_clientNum--; // 客戶連線數減一
			print(nickName + " > " + "已離線");
			_broadCastThread.removeClientThread(this);
			print("目前有　" + _clientNum + "　人在線上......");
			if (Car) {
				DeleteCar();
				_broadCastThread.addMessage("NC/" + setlectCAR_AL() + "|");
			}
		} catch (IOException e) {
			print(e.toString());
		}
	}

	private void Delete() {
		if (New) {
			try {
				print(nickName + " > " + "未使用");
				stmt.executeUpdate("DELETE FROM emrc WHERE emrc.number = '" + nickName + "'");
			} catch (SQLException e) {
				print(e.toString());
			}
			New = false;
		}
	}

	private void DeleteCar() {
		try {
			print(brand + " > " + "已登出");
			stmt.executeUpdate("DELETE FROM car WHERE car.car_brand = '" + brand + "'");
			brand = "";
		} catch (SQLException e) {
			print(e.toString());
		}

	}

	private String setlectBitmaps(int column) {
		String Bitmaps = "";
		try {
			ResultSet rs = stmt.executeQuery("SELECT " + getBitmap(column) + " FROM emrc WHERE number = " + nickName);
			while (rs.next()) {
				Bitmaps = rs.getString(getBitmap(column));
			}
		} catch (SQLException e) {
			print(e.toString());
		}
		return Bitmaps;
	}

	private String DateInput() {
		String Path = null;
		// C:\AppServ\www\image\photo
		Path = "C:/AppServ/www/image/photo/" + getCalendar() + ".jpg";
		Adler32 inChecker = new Adler32();
		Adler32 outChecker = new Adler32();
		CheckedDataInput in = null;
		CheckedDataOutput out = null;
		try {
			DataInputStream dis = new DataInputStream(_skt.getInputStream());
			int size = dis.readInt();
			in = new CheckedDataInput(new DataInputStream(_skt.getInputStream()), inChecker);
			out = new CheckedDataOutput(new DataOutputStream(new FileOutputStream(new File(Path))), outChecker);
			byte[] data = new byte[size];
			in.readFully(data);
			out.write(data);
		} catch (IOException e) {
			print(nickName + " > " + "ERR-DateInput:" + e.toString());
		}
		return Path;
	}

	class Adler32 implements Checksum {
		private int value = 1;

		/*
		 * BASE is the largest prime number smaller than 65536 NMAX is the
		 * largest n such that 255n(n+1)/2 + (n+1)(BASE-1) <= 2^32-1
		 */
		private static final int BASE = 65521;
		private static final int NMAX = 5552;

		/**
		 * Update current Adler-32 checksum given the specified byte.
		 */
		public void update(int b) {
			int s1 = value & 0xffff;
			int s2 = (value >> 16) & 0xffff;
			s1 += b & 0xff;
			s2 += s1;
			value = ((s2 % BASE) << 16) | (s1 % BASE);
		}

		/**
		 * Update current Adler-32 checksum given the specified byte array.
		 */
		public void update(byte[] b, int off, int len) {
			int s1 = value & 0xffff;
			int s2 = (value >> 16) & 0xffff;

			while (len > 0) {
				int k = len < NMAX ? len : NMAX;
				len -= k;
				while (k-- > 0) {
					s1 += b[off++] & 0xff;
					s2 += s1;
				}
				s1 %= BASE;
				s2 %= BASE;
			}
			value = (s2 << 16) | s1;
		}

		/**
		 * Reset Adler-32 checksum to initial value.
		 */
		public void reset() {
			value = 1;
		}

		/**
		 * Returns current checksum value.
		 */
		public long getValue() {
			return (long) value & 0xffffffff;
		}
	}

	interface Checksum {
		/**
		 * Updates the current checksum with the specified byte.
		 */
		public void update(int b);

		/**
		 * Updates the current checksum with the specified array of bytes.
		 */
		public void update(byte[] b, int off, int len);

		/**
		 * Returns the current checksum value.
		 */
		public long getValue();

		/**
		 * Resets the checksum to its initial value.
		 */
		public void reset();
	}

	class CheckedDataOutput {
		private Checksum cksum;
		private DataOutput out;

		public CheckedDataOutput(DataOutput out, Checksum cksum) {
			this.cksum = cksum;
			this.out = out;
		}

		public void write(int b) throws IOException {
			out.write(b);
			cksum.update(b);
		}

		public void write(byte[] b) throws IOException {
			out.write(b, 0, b.length);
			cksum.update(b, 0, b.length);
		}

		public void write(byte[] b, int off, int len) throws IOException {
			out.write(b, off, len);
			cksum.update(b, off, len);
		}

		public Checksum getChecksum() {
			return cksum;
		}
	}

	class CheckedDataInput {
		private Checksum cksum;
		private DataInput in;

		public CheckedDataInput(DataInput in, Checksum cksum) {
			this.cksum = cksum;
			this.in = in;
		}

		public byte readByte() throws IOException {
			byte b = in.readByte();
			cksum.update(b);
			return b;
		}

		public void readFully(byte[] b) throws IOException {
			in.readFully(b, 0, b.length);
			cksum.update(b, 0, b.length);
		}

		public void readFully(byte[] b, int off, int len) throws IOException {
			in.readFully(b, off, len);
			cksum.update(b, off, len);
		}

		public Checksum getChecksum() {
			return cksum;
		}
	}

	private void setMSG_toCAR() {
		// TODO 車輛待命時搜尋資料
		if (brand.length() > 0) {
			get_brand();
		}
	}

	private void setCAR_toDB(String tag) {
		// Car update
		int tmp = 4;
		String msg = tag.substring(tmp).substring(0, tag.substring(tmp).indexOf('|'));
		tmp = 0;
		String unit = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/'));// 收治單位
		tmp += unit.length() + 1;
		String car_brand = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/'));// 車牌
		tmp += car_brand.length() + 1;
		String count = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/'));// 車輛狀態
		tmp += count.length() + 1;
		String number = "";
		if (msg.substring(tmp).indexOf('/') > 0) {
			number = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/'));
		}
		int car_count = Integer.parseInt(count);
		if (!Car) {
			Car = true;
			brand = car_brand;
			if (!searchCAR(car_brand)) {
				// New
				addCAR(unit, car_brand, count, number, true);
				if (!searchHistory(car_brand)) {
					addCAR(unit, car_brand, count, number, false);
				}
			} else {
				// Error
				updateCAR(unit, car_brand, count, number);
			}
		} else {
			if (car_count != 8) {
				// Update
				if (car_brand != brand) {
					updateCARdate(unit, car_brand, count, number);
				} else {
					updateCAR(unit, car_brand, count, number);
				}
			} else {
				// Logout(8離線)
				Car = false;
				DeleteCar();
			}
		}
		print(nickName + " > " + unit + '/' + car_brand + '/' + car_status[car_count] + '/' + number);
		_broadCastThread.addMessage("NC/" + setlectCAR_AL() + "|");
	}

	private void setGPS_toDB(String tag) {
		int tmp = 4;
		String msg = tag.substring(tmp).substring(0, tag.substring(tmp).indexOf('|'));
		tmp = 0;
		String numbers = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/'));// 流水號
		tmp += numbers.length() + 1;
		String Latitude = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/'));// 緯度
		tmp += Latitude.length() + 1;
		String Longitude = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/'));// 經度
		String GPS = Latitude + ',' + Longitude + '/';
		print(nickName + " > " + tag);
		updateGPS(numbers, GPS);
	}

	private void setRUN_toDB(String tag) {
		int tmp = 4;
		String msg = tag.substring(tmp).substring(0, tag.substring(tmp).indexOf('|'));
		tmp = 0;
		String numbers = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/'));// 流水號
		tmp += numbers.length() + 1;
		String hosp = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/'));// 醫院
		tmp += hosp.length() + 1;
		String trends = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/'));// 即時狀況
		ArrayList<String> list = new ArrayList<String>();
		list.add("null"); // 0
		list.add("成大醫院"); // 1
		list.add("台南市醫"); // 2
		list.add("衛部臺南"); // 3
		list.add("台南榮總"); // 4
		list.add("台南新樓"); // 5
		list.add("麻豆新樓"); // 6
		list.add("奇美醫院"); // 7
		list.add("柳營奇美"); // 8
		list.add("佳里奇美"); // 9
		list.add("郭綜合"); // 10
		list.add("安南醫院"); // 11 安南醫院
		list.add("衛部新化"); // 12 衛部新化
		list.add("衛部新營"); // 13 衛部新營

		list.add("秀傳醫院"); // 14 秀傳醫院
		list.add("彰化基督教醫院"); // 15 彰化基督教醫院
		list.add("彰濱秀傳醫院"); // 16 彰濱秀傳醫院
		list.add("二林基督教醫院"); // 17 二林基督教醫院
		list.add("鹿港基督教醫院"); // 18 鹿港基督教醫院
		list.add("衛福部彰化醫院"); // 19 衛福部彰化醫院
		list.add("卓醫院"); // 20 卓醫院
		list.add("仁和醫院"); // 21 仁和醫院
		list.add("員榮醫院"); // 22 員榮醫院
		list.add("道周醫院"); // 23 道周醫院
		list.add("道安醫院"); // 24 道安醫院

		list.add("臺大醫院雲林分院"); // 25 臺大醫院雲林分院
		list.add("若瑟醫院"); // 26 若瑟醫院
		list.add("中醫大北港醫院"); // 27 中醫大北港醫院
		list.add("雲林基督教醫院"); // 28 雲林基督教醫院
		list.add("成大醫院斗六分院"); // 29 成大醫院斗六分院
		list.add("雲林長庚醫院"); // 30 雲林長庚醫院
		print(nickName + " > " + trends);
		updateRUN(numbers, trends);
		_broadCastThread.addMessage("UP/" + numbers + "/" + trends + "/|");
	}

	private void setNFC_toDB(String tag) {
		int tmp = 4;
		String msg = tag.substring(tmp).substring(0, tag.substring(tmp).indexOf('|'));
		print(nickName + " > " + msg);
		tmp = 0;
		String numbers = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/'));// 流水號
		tmp += numbers.length() + 1;

		String genders = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/'));// 性別
		tmp += 2;

		String info = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('║'));// 基本資料
		tmp += info.length() + 1;

		String inju_f = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('║'));// 前:標記點
		tmp += inju_f.length() + 1;

		String inju_b = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('║'));// 後:標記點
		tmp += inju_b.length() + 1;

		String vita = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('║'));// 生理資訊
		tmp += vita.length() + 1;

		String leve = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/'));// 傷患等級
		tmp += 2;

		String emrc = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/'));// 快速檢傷
		tmp += (emrc + "").length() + 1;

		String hosp = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/'));// 醫院
		tmp += (hosp + "").length() + 1;

		String iden = "";
		if (msg.substring(tmp).indexOf('/') > 0) {
			iden = msg.substring(tmp).substring(0, msg.substring(tmp).indexOf('/'));// 身分證
		}
		if (numbers.length() > 5) {
			nickName = numbers;
		}
		if (setlectNO(numbers)) {
			updateNFC(numbers, genders, info + "|", inju_f, inju_b, vita, leve, emrc, hosp, iden);
		} else {
			addNFC(numbers, genders, info + "|", inju_f, inju_b, vita, leve, emrc, hosp, iden);
		}
		setRYGB_toDB();
	}

	private void getNFC_toDB() {
		try {
			// int i = 0;
			ResultSet rs = stmt.executeQuery(
					"SELECT number,gender,info_date,inju_front,inju_back,vita_date,leve_count,emrc_count,hosp_count,info_identity FROM emrc ORDER BY number DESC");
			while (rs.next()) {
				String leve = rs.getString("leve_count");

				if (!"5".equals(leve)) {
					System_sendMessage("C2/" + rs.getString("number") + '/' + rs.getString("gender") + '/'
							+ rs.getString("info_date") + '║' + rs.getString("inju_front") + '║'
							+ rs.getString("inju_back") + '║' + rs.getString("vita_date") + '║'
							+ rs.getString("leve_count") + '/' + rs.getString("emrc_count") + '/'
							+ rs.getString("hosp_count") + '/' + rs.getString("info_identity") + "/|");
				}
			}
			System_sendMessage("C2/END");
			// print("C2/END");
		} catch (SQLException e) {
			print(e.toString());
		}
	}

	private String getLine(String msg, int run, char key) {
		return msg.substring(run).substring(0, msg.substring(run).indexOf(key));
	}

	private void setRYGB_toDB() {
		int sR = 0, sY = 0, sG = 0, sB = 0, hR = 0, hY = 0, hG = 0, hB = 0, white = 0;
		int leve, hosp;
		String info, name, age, other, gender;
		try {
			ResultSet rs = stmt.executeQuery("SELECT gender,info_date,leve_count,hosp_count FROM emrc");
			while (rs.next()) {
				int v = 0, r = 0;
				info = rs.getString("info_date");
				name = getLine(info, 0, '/');
				r += name.length() + 1;
				age = getLine(info, r, '/');
				r += age.length() + 1;
				other = info.substring(r, info.length() - 1);
				gender = rs.getString("gender");
				leve = Integer.parseInt(rs.getString("leve_count"));
				hosp = Integer.parseInt(rs.getString("hosp_count"));
				if (gender.length() == 1) {
					v++;
				}
				if (other.length() > 0) {
					v++;
				}
				if (name.length() != 0) {
					if (name != "n") {
						v++;
					} else {
						v = 0;
					}
				}
				if (v > 0) {
					if (hosp != 0) {
						switch (leve) {
						case 0:
							hB++;
							break;
						case 1:
							hR++;
							break;
						case 2:
							hY++;
							break;
						case 3:
							hG++;
							break;
						default:
							white++;
							break;
						}
					} else {
						switch (leve) {
						case 0:
							sB++;
							break;
						case 1:
							sR++;
							break;
						case 2:
							sY++;
							break;
						case 3:
							sG++;
							break;
						default:
							white++;
							break;
						}
					}
				}
			}
			stmt.executeUpdate("UPDATE emrc_index SET s_red = '" + sR + "',s_yellow = '" + sY + "',s_green = '" + sG
					+ "',s_black = '" + sB + "',h_red = '" + hR + "',h_yellow = '" + hY + "',h_green = '" + hG
					+ "',h_black = '" + hB + "',white = '" + white + "' WHERE id = 20170320;");
			_broadCastThread.addMessage("c2");
		} catch (SQLException e) {
			print(e.toString());
		}
	}

	private void getRYGB_toDB() {
		String msg = "";
		try {
			ResultSet rs = stmt.executeQuery(
					"SELECT h_red,h_yellow,h_green,h_black,s_red,s_yellow,s_green,s_black,white FROM emrc_index");
			while (rs.next()) {
				msg = rs.getString("h_red") + '/' + rs.getString("h_yellow") + '/' + rs.getString("h_green") + '/'
						+ rs.getString("h_black") + '/' + rs.getString("s_red") + '/' + rs.getString("s_yellow") + '/'
						+ rs.getString("s_green") + '/' + rs.getString("s_black") + '/' + rs.getString("white") + '/';
			}
		} catch (SQLException e) {
			print(e.toString());
		}
		System_sendMessage("C2/" + msg);
		print("C2/" + msg);
	}

	private void getHospital() {
		String msg = "";
		try {
			ResultSet rs = stmt.executeQuery("SELECT hosp_count FROM emrc WHERE number = " + nickName + ";");
			while (rs.next()) {
				msg = rs.getString("hosp_count");
			}
		} catch (SQLException e) {
			print(e.toString());
		}
		if (msg.length() > 0) {
			System_sendMessage("C3/" + msg);
			print("C3/" + msg);
		} else {
			System_sendMessage("C3/0");
			print("C3/0");
		}
	}

	private void setPhoto_toDB(String filePath, int column) {
		try {
			stmt.executeUpdate(
					"UPDATE emrc SET " + getBitmap(column) + " = '" + filePath + "' WHERE number = " + nickName + ";");

		} catch (SQLException e) {
			print(e.toString());
		}
	}

	private Boolean setlectNO(String numbers) {
		String number = "0";
		boolean select = false;
		try {
			ResultSet rs = stmt.executeQuery("SELECT number FROM emrc WHERE number = '" + numbers + "'");
			while (rs.next()) {
				number = rs.getString("number");
			}
		} catch (SQLException e) {
			print(e.toString());
		}
		if (number.length() > 3) {
			select = true;
		}
		return select;
	}

	private Boolean searchHistory(String licenses) {
		String license = "";
		boolean select;
		try {

			ResultSet rs = stmt.executeQuery("SELECT car_brand FROM car_history WHERE car_brand = '" + licenses + "'");
			while (rs.next()) {
				license = rs.getString("car_brand");
			}
		} catch (SQLException e) {
			print(e.toString());
		}
		if (license.length() > 1) {
			select = true;
			return select;
		} else {
			select = false;
			return select;
		}
	}

	private Boolean searchCAR(String licenses) {
		String license = "";
		boolean select;
		try {

			ResultSet rs = stmt.executeQuery("SELECT car_brand FROM car WHERE car_brand = '" + licenses + "'");
			while (rs.next()) {
				license = rs.getString("car_brand");
			}
		} catch (SQLException e) {
			print(e.toString());
		}
		if (license.length() > 1) {
			select = true;
			return select;
		} else {
			select = false;
			return select;
		}
	}

	private String setlectCAR_AL() {
		String msg = "";
		int run = 0;
		ArrayList<String> list = new ArrayList<String>();
		list.clear();
		try {
			ResultSet rs = stmt.executeQuery("SELECT car_unit,car_brand,car_status FROM car");
			while (rs.next()) {
				run++;
				list.add(rs.getString("car_status") + '/' + rs.getString("car_brand") + '/' + rs.getString("car_unit")
						+ "/║");
			}
		} catch (SQLException e) {
			print(e.toString());
		}
		if (run != 0) {
			msg = run + "/";
			for (int i = 0; i < list.size(); i++) {
				msg += list.get(i);
			}
		}
		// print(msg);
		return msg;
	}

	private void updateGPS(String numbers, String new_gps) {
		// UPDATE MEDI_UPDATE
		String gps = "", unit = "";
		try {
			ResultSet rs = stmt.executeQuery("SELECT car_unit FROM car_history WHERE car_brand = '" + brand + "'");
			while (rs.next()) {
				unit = rs.getString("car_unit");
			}
			rs = stmt.executeQuery("SELECT trends_location FROM emrc WHERE number = " + numbers);
			while (rs.next()) {
				gps = rs.getString("trends_location");
			}
			if (gps != null) {
				gps += new_gps;
			} else {
				gps = new_gps;
			}
			if (Car) {
				stmt.executeUpdate("UPDATE emrc SET trends_location = '" + gps + "', medi_unit = '" + unit
						+ "', medi_brand = '" + brand + "' WHERE emrc.number = " + numbers + ";");
			}
		} catch (SQLException e) {
			print(e.toString());
		}
	}

	private void updateRUN(String numbers, String trends) {
		try {

			stmt.executeUpdate("UPDATE emrc SET trends_count = '" + trends + "' WHERE emrc.number = " + numbers + ";");

		} catch (SQLException e) {
			print(e.toString());
		}
	}

	private void updateCARdate(String unit, String license, String count, String number) {
		try {
			stmt.executeUpdate("UPDATE car SET car_unit = '" + unit + "', car_brand = '" + license + "', car_status = '"
					+ count + "', update_time = NOW(), car_number = '" + number + "' WHERE car.car_brand = '" + brand
					+ "';");
			brand = license;
		} catch (SQLException e) {
			print(e.toString());
		}
	}

	private void updateCAR(String unit, String license, String count, String number) {
		try {
			stmt.executeUpdate("UPDATE car SET car_unit = '" + unit + "', car_status = '" + count
					+ "', update_time = NOW(), car_number = '" + number + "' WHERE car.car_brand = '" + license + "';");
		} catch (SQLException e) {
			print(e.toString());
		}
	}

	private void updateNFC(String numbers, String genders, String info, String inju_f, String inju_b, String vita,
			String leve, String emrc, String hosp, String iden) {
		try {

			stmt.executeUpdate("UPDATE emrc SET gender = '" + genders + "', info_date = '" + info + "', inju_front = '"
					+ inju_f + "', inju_back = '" + inju_b + "', vita_date = '" + vita + "', leve_count = '" + leve
					+ "', emrc_count = '" + emrc + "', hosp_count = '" + hosp + "',info_identity = '" + iden
					+ "' WHERE emrc.number = " + numbers + ";");

		} catch (SQLException e) {
			print(e.toString());
		}
	}

	private void addNFC(String numbers, String genders, String info, String inju_f, String inju_b, String vita,
			String leve, String emrc, String hosp, String iden) {
		try {
			stmt.executeUpdate(
					"INSERT INTO emrc (number, gender, info_date, inju_front, inju_back, vita_date, leve_count, emrc_count, hosp_count, info_identity) VALUES "
							+ "('" + numbers + "','" + genders + "','" + info + "','" + inju_f + "','" + inju_b + "','"
							+ vita + "','" + leve + "','" + emrc + "','" + hosp + "','" + iden + "');");
		} catch (SQLException e) {
			print(e.toString());
		}
	}

	private void addCAR(String unit, String license, String count, String number, boolean car) {
		try {
			if (car) {
				stmt.executeUpdate("INSERT INTO car (car_unit, car_brand, car_status, update_time, car_number) VALUES "
						+ "('" + unit + "','" + license + "','" + count + "',NOW(), '" + number + "');");
			} else {
				stmt.executeUpdate("INSERT INTO car_history (car_unit, car_brand) VALUES " + "('" + unit + "','"
						+ license + "');");
			}
		} catch (SQLException e) {
			print(e.toString());
		}
	}

	private String get_NO() {
		New = true;
		String number = null;
		try {
			ResultSet rs = stmt.executeQuery("SELECT MAX(number)+1 FROM emrc");
			while (rs.next()) {
				number = rs.getString("MAX(number)+1");
			}
			int today = Integer.parseInt(getCalendarNO());
			if (null != number || !"".equals(number)) {
				if (number.length() > 5) {
					int no = Integer.parseInt(number.substring(0, 6));
					if (today == no) {
						// print("DB > NO:" + number);
					} else {
						// print("SET > NO:" + today + "00001");
						number = today + "00001";
					}
					stmt.executeUpdate(
							"INSERT INTO emrc (number, gender, info_date, inju_front, inju_back, vita_date, leve_count, emrc_count, hosp_count, medi_update) VALUES "
									+ "('" + number + "','2','//|','0=n/','0=n/','n/','5','0','0','0');");
				} else {
					number = today + "00001";
				}
			} else {
				number = today + "00001";
			}
		} catch (NumberFormatException | SQLException e) {
			print(e.toString());
		}
		return number;
	}

	private String getBitmap(int column) {
		String Path = "";
		switch (column) {
		case 0:
			Path = "bitmap_0";
			break;
		case 1:
			Path = "bitmap_1";
			break;
		case 2:
			Path = "bitmap_2";
			break;
		case 3:
			Path = "bitmap_3";
			break;
		case 4:
			Path = "bitmap_4";
			break;
		case 5:
			Path = "bitmap_5";
			break;
		case 6:
			Path = "bitmap_6";
			break;
		}
		return Path;
	}

	private void get_brand() {
		ArrayList<String> nb = new ArrayList<String>();
		ArrayList<String> hs = new ArrayList<String>();
		ArrayList<String> ls = new ArrayList<String>();
		ArrayList<String> hlist = new ArrayList<String>();
		hlist.add("null"); // 0
		hlist.add("成大醫院"); // 1
		hlist.add("台南市醫"); // 2
		hlist.add("衛部臺南"); // 3
		hlist.add("台南榮總"); // 4
		hlist.add("台南新樓"); // 5
		hlist.add("麻豆新樓"); // 6
		hlist.add("奇美醫院"); // 7
		hlist.add("柳營奇美"); // 8
		hlist.add("佳里奇美"); // 9
		hlist.add("郭綜合"); // 10
		hlist.add("安南醫院"); // 11 安南醫院
		hlist.add("衛部新化"); // 12 衛部新化
		hlist.add("衛部新營"); // 13 衛部新營

		hlist.add("秀傳醫院"); // 14 秀傳醫院
		hlist.add("彰化基督教醫院"); // 15 彰化基督教醫院
		hlist.add("彰濱秀傳醫院"); // 16 彰濱秀傳醫院
		hlist.add("二林基督教醫院"); // 17 二林基督教醫院
		hlist.add("鹿港基督教醫院"); // 18 鹿港基督教醫院
		hlist.add("衛福部彰化醫院"); // 19 衛福部彰化醫院
		hlist.add("卓醫院"); // 20 卓醫院
		hlist.add("仁和醫院"); // 21 仁和醫院
		hlist.add("員榮醫院"); // 22 員榮醫院
		hlist.add("道周醫院"); // 23 道周醫院
		hlist.add("道安醫院"); // 24 道安醫院

		hlist.add("臺大醫院雲林分院"); // 25 臺大醫院雲林分院
		hlist.add("若瑟醫院"); // 26 若瑟醫院
		hlist.add("中醫大北港醫院"); // 27 中醫大北港醫院
		hlist.add("雲林基督教醫院"); // 28 雲林基督教醫院
		hlist.add("成大醫院斗六分院"); // 29 成大醫院斗六分院
		hlist.add("雲林長庚醫院"); // 30 雲林長庚醫院

		ArrayList<String> list = new ArrayList<String>();

		try {
			ResultSet rs = stmt.executeQuery(
					"SELECT number,gender,info_date,inju_front,inju_back,vita_date,leve_count,emrc_count,hosp_count,info_identity FROM emrc WHERE "
							+ "medi_update = 0 AND medi_brand = '" + brand + "'");
			while (rs.next()) {
				nb.add(rs.getString("number"));
				String d = rs.getString("info_date");
				d = d.substring(0, d.length() - 1);
				ls.add(rs.getString("leve_count"));
				hs.add(rs.getString("hosp_count"));
				list.add(rs.getString("number") + '/' + rs.getString("gender") + '/' + d + '║'
						+ rs.getString("inju_front") + '║' + rs.getString("inju_back") + '║' + rs.getString("vita_date")
						+ '║' + rs.getString("leve_count") + '/' + rs.getString("emrc_count") + '/'
						+ rs.getString("hosp_count") + '/' + rs.getString("info_identity") + "/|");
			}
			if (list.size() != 0) {
				System_sendMessage("DC/000");
				for (int i = 0; i < list.size(); i++) {
					print(brand + " > " + list.get(i));
					System_sendMessage("DC/" + list.get(i));
				}
				System_sendMessage("DC/END");
			}
			for (int i = 0; i < nb.size(); i++) {
				stmt.executeUpdate("UPDATE emrc SET medi_update = '1' WHERE number = " + nb.get(i) + ";");
				int leve = Integer.parseInt(ls.get(i));
				if (leve < 5 && leve != 0) {
					int hosp = Integer.parseInt(hs.get(i));
					String hsp = hlist.get(hosp);
					String[] s = { "b", "r", "y", "g" };
					int max = 0;
					if (hosp >= 25) {
						ResultSet ra = stmt
								.executeQuery("SELECT " + s[leve] + " FROM hospital_yunlin WHERE name = '" + hsp + "'");
						while (ra.next()) {
							max = Integer.parseInt(ra.getString(s[leve]));
						}
						max--;
						if (max >= 0) {
							stmt.executeUpdate("UPDATE hospital_yunlin SET " + s[leve] + " = '" + max
									+ "' WHERE name = '" + hsp + "';");
						}
					} else if (hosp >= 14) {
						ResultSet rb = stmt.executeQuery(
								"SELECT " + s[leve] + " FROM hospital_changhua WHERE name = '" + hsp + "'");
						while (rb.next()) {
							max = Integer.parseInt(rb.getString(s[leve]));
						}
						max--;
						if (max >= 0) {
							stmt.executeUpdate("UPDATE hospital_changhua SET " + s[leve] + " = '" + max
									+ "' WHERE name = '" + hsp + "';");
						}
					}
				}
			}
		} catch (NumberFormatException | SQLException e) {
			print(e.toString());
		}
	}

	@SuppressWarnings("unused")
	private static String getCalendarTime() {
		String M, D, H, I;
		Calendar calendar = Calendar.getInstance();
		int m = (calendar.get(Calendar.MONTH) + 1);
		int d = calendar.get(Calendar.DAY_OF_MONTH);
		int h = calendar.get(Calendar.HOUR_OF_DAY);
		int i = calendar.get(Calendar.MINUTE);

		if (m < 10) {
			M = "0" + m;
		} else {
			M = "" + m;
		}
		if (d < 10) {
			D = "0" + d;
		} else {
			D = "" + d;
		}
		if (h < 10) {
			H = "0" + h;
		} else {
			H = "" + h;
		}
		if (i < 10) {
			I = "0" + i;
		} else {
			I = "" + i;
		}
		String time = M + "月" + D + "日" + H + "時" + I + "分";
		return time;
	}

	private String getTimer() {

		String H, I, S;
		Calendar calendar = Calendar.getInstance();
		// int y = Integer.parseInt((calendar.get(Calendar.YEAR) +
		// "").substring(2, 4));
		// int m = (calendar.get(Calendar.MONTH) + 1);
		// int d = calendar.get(Calendar.DAY_OF_MONTH);
		int h = calendar.get(Calendar.HOUR_OF_DAY);
		int i = calendar.get(Calendar.MINUTE);
		int s = calendar.get(Calendar.SECOND);
		// if (y < 10) {
		// Y = "0" + y;
		// } else {
		// Y = "" + y;
		// }
		// if (m < 10) {
		// M = "0" + m;
		// } else {
		// M = "" + m;
		// }
		// if (d < 10) {
		// D = "0" + d;
		// } else {
		// D = "" + d;
		// }
		if (h < 10) {
			H = "0" + h;
		} else {
			H = "" + h;
		}
		if (i < 10) {
			I = "0" + i;
		} else {
			I = "" + i;
		}
		if (s < 10) {
			S = "0" + s;
		} else {
			S = "" + s;
		}
		// 1612231444
		String time = H + ":" + I + ":" + S + "";
		return time;
	}

	private static String getCalendarNO() {

		String Y, M, D;
		Calendar calendar = Calendar.getInstance();
		int y = Integer.parseInt((calendar.get(Calendar.YEAR) + "").substring(2, 4));
		int m = (calendar.get(Calendar.MONTH) + 1);
		int d = calendar.get(Calendar.DAY_OF_MONTH);
		if (y < 10) {
			Y = "0" + y;
		} else {
			Y = "" + y;
		}
		if (m < 10) {
			M = "0" + m;
		} else {
			M = "" + m;
		}
		if (d < 10) {
			D = "0" + d;
		} else {
			D = "" + d;
		}
		return Y + M + D;
	}

	private String getCalendar() {
		String Y, M, D, H, I, S;
		Calendar calendar = Calendar.getInstance();
		int y = Integer.parseInt((calendar.get(Calendar.YEAR) + "").substring(2, 4));
		int m = (calendar.get(Calendar.MONTH) + 1);
		int d = calendar.get(Calendar.DAY_OF_MONTH);
		int h = calendar.get(Calendar.HOUR_OF_DAY);
		int i = calendar.get(Calendar.MINUTE);
		int s = calendar.get(Calendar.SECOND);
		if (y < 10) {
			Y = "0" + y;
		} else {
			Y = "" + y;
		}
		if (m < 10) {
			M = "0" + m;
		} else {
			M = "" + m;
		}
		if (d < 10) {
			D = "0" + d;
		} else {
			D = "" + d;
		}
		if (h < 10) {
			H = "0" + h;
		} else {
			H = "" + h;
		}
		if (i < 10) {
			I = "0" + i;
		} else {
			I = "" + i;
		}
		if (s < 10) {
			S = "0" + s;
		} else {
			S = "" + s;
		}
		// 1612231444
		String time = Y + M + D + H + I + S + "";
		return time;
	}

	private void print(String message) {
		System.out.println(message);
	}

	private Runnable sendData = new Runnable() { // INPUT

		public void run() {
			try {
				FileInputStream fin = new FileInputStream(file);
				DataOutputStream dos = new DataOutputStream(_skt.getOutputStream());
				int size = fin.available();
				// System.out.println("size = " + filesize);
				byte[] data = new byte[size];
				fin.read(data);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					print(e.toString());
				}
				dos.writeInt(size);
				dos.write(data);
				fin.close();
				print("FileWriteFinish!");
			} catch (IOException e) {
				print(e.toString());
			}
		}
	};

	private Runnable autoCheck = new Runnable() {
		public void run() {
			System_sendMessage("s1");
			try {
				Thread.sleep(3000);
			} catch (Exception e) {
				print(e.toString());
			}
			if (!sktLink) {
				print(nickName + " > 沒有回應");
			}
		}
	};
}
