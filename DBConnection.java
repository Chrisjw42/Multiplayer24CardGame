import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
//import java.util.Scanner;
import java.util.LinkedList;
import java.util.Random;

public class DBConnection {
	private static final String DB_HOST = "localhost";
	private static final String DB_USER = "c0402@localhost";
	private static final String DB_PASS = "c0402PASS";
	private static final String DB_NAME = "c0402";
	private Connection conn;
	private Random rng;

	public static void main(String[] args) {
		try {
			// redundant
			new DBConnection();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e) {
			System.err.println("Connection failed: " + e);
		}
	}

	public DBConnection() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		conn = DriverManager
				.getConnection("jdbc:mysql://" + DB_HOST + "/" + DB_NAME + "?user=" + DB_USER + "&password=" + DB_PASS);
		System.out.println("DB Connected successfully");
		rng = new Random();
	}
	
	private void TestDBAddGame() {
		//Testing
		Game thisGame = new Game(new Player("q", "123"));
		thisGame.AddPlayer(new Player("w", "234"));
		thisGame.SetCards(new String[]{"10","11","6","3"});
		thisGame.InitialiseId(getValidId("game"));
		addGameToDB(thisGame);
	}

	private LinkedList<String[]> listPlayers() {
		LinkedList<String[]> results = new LinkedList<String[]>();
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT playerId, playerName FROM player");
			while (rs.next()) {
				// Stored as in in table, convert to string on the way out. Only an int in table
				String[] row = { Integer.toString(rs.getInt(1)), rs.getString(2) };
				results.add(row);
			}
		} catch (SQLException e) {
			System.err.println("Error listing records: " + e);
		}
		return results;
	}

	private LinkedList<String[]> getIdList(String table) {
		LinkedList<String[]> results = new LinkedList<String[]>();
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT " + table + "Id FROM " + table);
			while (rs.next()) {
				// Stored as in in table, convert to string on the way out. Only an int in table
				String[] row = { Integer.toString(rs.getInt(1)) };
				results.add(row);
			}
		} catch (SQLException e) {
			System.err.println("Error listing records: " + e);
		}
		return results;
	}

	private boolean isIdFree(String id, String table) {
		LinkedList<String[]> ids = getIdList(table);
		for (int i = 0; i < ids.size(); i++) {
			if (ids.get(i)[0].equals(id)) {
				System.out.println("MATCH, this Id is NOT free.");
				return false;
			}
		}
		return true;
	}

	public String getValidId(String table) {
		boolean ok = false;
		String strID = "";

		// rng an index until we find one that doesn't exist already
		while (!ok) {
			int iD = rng.nextInt(999999999);
			strID = Integer.toString(iD);
			ok = isIdFree(strID, table);
		}
		return strID;
	}
	
	public boolean addGameToDB(Game game) {
		try {
			// Question marks are used for passing parameters
			PreparedStatement stmt = conn
					.prepareStatement("INSERT INTO game (gameId, cards, player1) VALUES (?, ?, ?)");
			stmt.setString(1, game.GetId());
			stmt.setString(2, game.GetCardsString("^"));
			stmt.setString(3, game.GetPlayers()[0].id);
			stmt.execute();
			
			// Now we add the individual players
			int nPlayers = game.GetNumberOfPlayers();
			
			// Start from i=1 (player 2)
			for (int i = 1; i < nPlayers; i++) {
				int  n = i + 1;
				
				// Sorry for this structure, can't pass the right kind of string programatically
				if (n == 2) {
					stmt = conn.prepareStatement("UPDATE game SET player2 = ? WHERE gameId = ?");
					stmt.setInt(1, Integer.parseInt(game.GetPlayers()[i].id));
					stmt.setInt(2, Integer.parseInt(game.GetId()));					
				}
				else if (n == 3){
					stmt = conn.prepareStatement("UPDATE game SET player3 = ? WHERE gameId = ?");
					stmt.setInt(1, Integer.parseInt(game.GetPlayers()[i].id));
					stmt.setInt(2, Integer.parseInt(game.GetId()));					
				}
				else if (n == 4) {
					stmt = conn.prepareStatement("UPDATE game SET player4 = ? WHERE gameId = ?");
					stmt.setInt(1, Integer.parseInt(game.GetPlayers()[i].id));
					stmt.setInt(2, Integer.parseInt(game.GetId()));					
				}
				int rows = stmt.executeUpdate();
			}
		} catch (SQLException | IllegalArgumentException e) {
			System.err.println("Error inserting record: " + e);
			return false;
		}
		return true;
	}

	public boolean addPlayerToTable(String playerId, String playerName, String playerPwd) {
		try {
			// Question marks are used for passing parameters
			PreparedStatement stmt = conn
					.prepareStatement("INSERT INTO player (playerId, playerName, playerPassword) VALUES (?, ?, ?)");
			stmt.setString(1, playerId);
			stmt.setString(2, playerName);
			stmt.setString(3, playerPwd);
			stmt.execute();
		} catch (SQLException | IllegalArgumentException e) {
			System.err.println("Error inserting record: " + e);
			return false;
		}
		return true;
	}

	public String getPlayerId(String name) {
		LinkedList<String[]> players = listPlayers();
		for (int i = 0; i < players.size(); i++) {
			if (players.get(i)[1].equals(name)) {
				return players.get(i)[0];
			}
		}

		System.err.println("The player " + name + "was not found, error!");
		return null;
	}

	public boolean doesPlayerExist(String playerName) {
		LinkedList<String[]> players = listPlayers();
		for (int i = 0; i < players.size(); i++) {
			if (players.get(i)[1].equals(playerName)) {
				System.out.println("MATCH, this player DOES exist.");
				return true;
			}
		}
		return false;
	}

	// NOTE: PlayerName must be unique
	public boolean checkPlayerPwd(String playerName, String playerPwd) {
		if (doesPlayerExist(playerName) == false) {
			return false;
		}
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT playerName, playerPassword FROM player");
			while (rs.next()) {
				String[] row = { rs.getString(1), rs.getString(2) };

				// Found player, check pwd matches
				if (row[0].equals(playerName)) {
					if (row[1].equals(playerPwd)) {
						return true;
					} else {
						return false;
					}
				}
			}
			return false;
		} catch (SQLException e) {
			System.err.println("Error listing records: " + e);
			return false;
		}
	}

	//// EXAMPLE METHODS FROM ASSIGNMENT 2

	private void read(String name) {
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT birthday FROM c0402_2017_t4 WHERE name = ?");
			stmt.setString(1, name);

			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				System.out.println("Birthday of " + name + " is on " + rs.getDate(1).toString());
			} else {
				System.out.println(name + " not found!");
			}
		} catch (SQLException e) {
			System.err.println("Error reading record: " + e);
		}
	}

	private void update(String name, String birthday) {
		try {
			PreparedStatement stmt = conn.prepareStatement("UPDATE c0402_2017_t4 SET birthday = ? WHERE name = ?");
			stmt.setDate(1, java.sql.Date.valueOf(birthday));
			stmt.setString(2, name);

			int rows = stmt.executeUpdate();
			if (rows > 0) {
				System.out.println("Birthday of " + name + " updated");
			} else {
				System.out.println("No birthdays updated, name: " + name + " not found!");
			}
		} catch (SQLException e) {
			System.err.println("Error reading record: " + e);
		}
	}

	private void delete(String name) {
		try {
			PreparedStatement stmt = conn.prepareStatement("DELETE FROM c0402_2017_t4 WHERE name = ?");
			stmt.setString(1, name);
			int rows = stmt.executeUpdate();
			if (rows > 0) {
				System.out.println("Record of " + name + " removed");
			} else {
				System.out.println("No removals, name: " + name + " not found!");
			}
		} catch (SQLException | IllegalArgumentException e) {
			System.err.println("Error inserting record: " + e);
		}
	}

	private void birthday(String birthday) {
		try {
			PreparedStatement stmt = conn
					.prepareStatement("SELECT name, birthday FROM c0402_2017_t4 WHERE birthday = ?");

			stmt.setString(1, birthday);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				System.out.println("Birthday of " + rs.getString(1) + " is on " + rs.getDate(2).toString());
			}
		} catch (SQLException e) {
			System.out.println("Failed to complete SQL statement: " + e);
		}
	}
}
