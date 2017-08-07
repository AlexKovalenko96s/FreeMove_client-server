package ua.kas.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

public class EchoThread extends Thread {

	protected Socket socket;

	private String key;

	private BufferedReader in;
	private PrintWriter out;

	private byte[] imgByte = null;

	public EchoThread(Socket clientSocket) {
		this.socket = clientSocket;
	}

	public void run() {

		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String line;
		try {
			while (true) {
				line = in.readLine();

				if (line.equals("END"))
					break;

				System.out.println("Request - " + line);

				key = line.substring(0, line.indexOf("&"));
				line = line.substring(line.indexOf("&") + 1);

				if (key.equals("login")) {
					login(line);
				} else if (key.equals("mustSee")) {
					mustSee(line);
				} else if (key.equals("signUp")) {
					signUp(line);
				} else if (key.equals("mustSeeItem")) {
					mustSeeItem(line);
				} else if (key.equals("like")) {
					like(line);
				}
				break;
			}
		} catch (IOException | ClassNotFoundException | SQLException ex) {
		}
	}

	private void login(String line) throws ClassNotFoundException, IOException {
		Class.forName("org.sqlite.JDBC");
		Connection connect;
		try {
			connect = DriverManager.getConnection("jdbc:sqlite::resource:ua/kas/main/freeMove.db");
			String query = "SELECT id FROM users WHERE login = ? and password = ?";
			PreparedStatement statement = connect.prepareStatement(query);
			String login = line.substring(0, line.indexOf("&"));
			String password = line.substring(line.indexOf("&") + 1);

			statement.setString(1, login);
			statement.setString(2, password);
			ResultSet res = statement.executeQuery();

			String outMessage = "";
			if (res.next()) {
				outMessage = res.getString("id");
			} else {
				outMessage = "0";
			}

			out.println(outMessage);

			connect.close();

			socket.close();

			System.out.println("Finish - login!");
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	private void mustSee(String line) throws ClassNotFoundException, SQLException, IOException {
		Class.forName("org.sqlite.JDBC");
		Connection connect = DriverManager.getConnection("jdbc:sqlite::resource:ua/kas/main/freeMove.db");
		String query = "SELECT name FROM mustSee";
		PreparedStatement statement = connect.prepareStatement(query);

		ResultSet res = statement.executeQuery();

		String names = "";

		while (res.next()) {
			names += res.getString("name") + "&";
		}

		names = names.substring(0, names.length() - 1);

		out.println(names);

		connect.close();

		socket.close();

		System.out.println("Finish - mustSee!");
	}

	private void signUp(String line) throws ClassNotFoundException, IOException {
		Class.forName("org.sqlite.JDBC");
		Connection connect;
		try {
			connect = DriverManager.getConnection("jdbc:sqlite::resource:ua/kas/main/freeMove.db");

			String login = line.substring(0, line.indexOf("&"));
			line = line.substring(line.indexOf("&") + 1);
			String email = line.substring(0, line.indexOf("&"));
			line = line.substring(line.indexOf("&") + 1);

			String query = "SELECT * FROM users WHERE login = ?";
			PreparedStatement statement = connect.prepareStatement(query);

			statement.setString(1, login);

			ResultSet res = statement.executeQuery();

			while (res.next()) {
				out.println("errorLogin");

				System.out.println("Finish - signIn!");

				connect.close();
				socket.close();
				return;
			}

			query = "SELECT * FROM users WHERE email = ?";
			PreparedStatement statementSecond = connect.prepareStatement(query);

			statementSecond.setString(1, email);

			ResultSet resSecond = statementSecond.executeQuery();

			while (resSecond.next()) {
				out.println("errorEmail");

				System.out.println("Finish - signIn!");

				connect.close();
				socket.close();
				return;
			}

			query = "INSERT INTO users (login, email, password) VALUES (?,?,?)";
			PreparedStatement statementThird = connect.prepareStatement(query);
			statementThird.setString(1, login);
			statementThird.setString(2, email);
			statementThird.setString(3, line);

			statementThird.executeUpdate();

			out.println("complete");

			connect.close();
			socket.close();

			System.out.println("Finish - signIn!");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void mustSeeItem(String line) throws ClassNotFoundException, SQLException, IOException {
		Class.forName("org.sqlite.JDBC");
		Connection connect = DriverManager.getConnection("jdbc:sqlite::resource:ua/kas/main/freeMove.db");
		String query = "SELECT * FROM mustSee WHERE name = ?";
		PreparedStatement statement = connect.prepareStatement(query);

		String name = line.substring(0, line.indexOf("&"));
		line = line.substring(line.indexOf("&") + 1);
		String type = line.substring(0, line.indexOf("&"));
		line = line.substring(line.indexOf("&") + 1);
		String userId = getUserId(line, connect);

		System.out.println(userId);

		String itemId = "";

		statement.setString(1, name);

		ResultSet res = statement.executeQuery();

		String string = "";

		while (res.next()) {
			itemId = res.getString("id");
			string = res.getString("name") + "&";
			string += res.getString("location") + "&";
			string += res.getString("number") + "&";
			string += res.getString("email");
			imgByte = res.getBytes("img");
		}

		string += "&" + coutLike(name, type) + "&";

		if (!userId.equals("Not authorized")) {
			string += checkLike(userId, itemId, type);
		} else {
			string += "Not authorized";
		}

		byte[] encoded = Base64.getEncoder().encode(imgByte);

		string += "&" + new String(encoded);

		out.println(string);

		connect.close();
		socket.close();

		System.out.println("Finish - mustSeeItem!");
	}

	public void like(String line) throws ClassNotFoundException, IOException {

		String type = line.substring(0, line.indexOf("&"));
		line = line.substring(line.indexOf("&") + 1);
		String name = line.substring(0, line.indexOf("&"));
		line = line.substring(line.indexOf("&") + 1);
		String user = line;

		String userId = "";
		String itemId = "";

		Class.forName("org.sqlite.JDBC");
		Connection connect;

		try {
			connect = DriverManager.getConnection("jdbc:sqlite::resource:ua/kas/main/freeMove.db");
			String query = "SELECT id FROM users WHERE login = ?";
			PreparedStatement statement = connect.prepareStatement(query);

			statement.setString(1, user);

			ResultSet res = statement.executeQuery();

			while (res.next()) {
				userId = res.getString("id");
			}

			statement.close();
			res.close();
			connect.close();

			Connection connectSecond = DriverManager.getConnection("jdbc:sqlite::resource:ua/kas/main/freeMove.db");
			query = "SELECT id FROM " + type + " WHERE name = ?";
			PreparedStatement statementSecond = connectSecond.prepareStatement(query);

			statementSecond.setString(1, name);

			ResultSet resSecond = statementSecond.executeQuery();

			while (resSecond.next()) {
				itemId = resSecond.getString("id");
			}

			statementSecond.close();
			resSecond.close();
			connectSecond.close();

			if ((!itemId.equals("")) && (!userId.equals(""))) {
				Connection connectThird = DriverManager.getConnection("jdbc:sqlite::resource:ua/kas/main/freeMove.db");
				query = "SELECT id FROM likes WHERE userId = ? and itemId = ? and itemType = ?";
				PreparedStatement statementThird = connectThird.prepareStatement(query);

				statementThird.setInt(1, Integer.parseInt(userId));
				statementThird.setInt(2, Integer.parseInt(itemId));
				statementThird.setString(3, type);

				ResultSet resThird = statementThird.executeQuery();

				while (resThird.next()) {
					deleteLike(userId, itemId, type, connectThird);

					String count = coutLike(name, type);
					String checkLike = checkLike(userId, itemId, type);

					out.println(count + "&" + checkLike);

					resThird.close();
					connectThird.close();

					socket.close();
					System.out.println("Finish - like!");
					return;
				}

				statementThird.close();
				resThird.close();
				connectThird.close();

				Connection conFourth = DriverManager.getConnection("jdbc:sqlite::resource:ua/kas/main/freeMove.db");

				query = "insert into likes (userId, itemId, itemType) values (?,?,?)";
				PreparedStatement statementFourth = conFourth.prepareStatement(query);

				statementFourth.setInt(1, Integer.parseInt(userId));
				statementFourth.setInt(2, Integer.parseInt(itemId));
				statementFourth.setString(3, type);

				statementFourth.execute();

				String count = coutLike(name, type);
				String checkLike = checkLike(userId, itemId, type);

				out.println(count + "&" + checkLike);

				statementFourth.close();
				conFourth.close();

				socket.close();

				System.out.println("Finish - like!");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public String coutLike(String itemName, String type) throws ClassNotFoundException {
		String itemId = "";
		String count = "";

		Class.forName("org.sqlite.JDBC");
		Connection connect;
		try {
			connect = DriverManager.getConnection("jdbc:sqlite::resource:ua/kas/main/freeMove.db");
			String query = "SELECT id FROM " + type + " WHERE name = ?";
			PreparedStatement statement = connect.prepareStatement(query);

			statement.setString(1, itemName);

			ResultSet res = statement.executeQuery();

			while (res.next()) {
				itemId = res.getString("id");
			}
			query = "SELECT COUNT(*) FROM likes WHERE itemType = ? AND itemId = ?";
			PreparedStatement statementSecond = connect.prepareStatement(query);

			statementSecond.setString(1, type);
			statementSecond.setString(2, itemId);

			ResultSet resSecond = statementSecond.executeQuery();
			while (resSecond.next()) {
				count = resSecond.getString("COUNT(*)");
			}

			connect.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return count;
	}

	public String checkLike(String userId, String itemId, String type) throws ClassNotFoundException {
		Class.forName("org.sqlite.JDBC");
		Connection connect;
		String like = "Not authorized";
		try {
			connect = DriverManager.getConnection("jdbc:sqlite::resource:ua/kas/main/freeMove.db");
			String query = "SELECT * FROM likes WHERE userId = ? and itemId = ? and itemType = ?";
			PreparedStatement statement = connect.prepareStatement(query);

			statement.setInt(1, Integer.parseInt(userId));
			statement.setInt(2, Integer.parseInt(itemId));
			statement.setString(3, type);

			ResultSet res = statement.executeQuery();

			while (res.next()) {
				like = "like";
			}
			System.out.println(like);

			connect.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return like;
	}

	public void deleteLike(String userId, String itemId, String type, Connection connect)
			throws ClassNotFoundException {
		Class.forName("org.sqlite.JDBC");
		try {
			String query = "DELETE FROM likes WHERE userId = ? and itemId = ? and itemType = ?";
			PreparedStatement statement = connect.prepareStatement(query);

			statement.setInt(1, Integer.parseInt(userId));
			statement.setInt(2, Integer.parseInt(itemId));
			statement.setString(3, type);

			statement.execute();

			connect.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public String getUserId(String userName, Connection connect) {
		if (!userName.equals("Not authorized")) {
			String userId = "";
			try {
				String query = "SELECT * FROM users WHERE login = ?";
				PreparedStatement statement = connect.prepareStatement(query);

				statement.setString(1, userName);

				ResultSet res = statement.executeQuery();

				while (res.next()) {
					userId = res.getString("id");
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}

			return userId;
		}
		return "Not authorized";
	}
}
