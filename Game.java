import java.util.Random;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class Game {
	private String id;
	private Player[] players;
	private int[] cards;
	private Random rand;
	private String[] suits;
	private String[] answers;
	private int[] scores;

	ScriptEngineManager manager;
	ScriptEngine engine;

	private Player EmptyPlayer;

	// new game requires minimum 1 player
	public Game(Player player) {
		rand = new Random();
		id = "unassigned";

		manager = new ScriptEngineManager();
		engine = manager.getEngineByName("js");

		// Dummy player
		EmptyPlayer = new Player("emptyplayer", "-1");
		
		suits = new String[] { "spades", "clubs", "diamonds", "hearts" };
		cards = new int[4];
		players = new Player[4];
		answers = new String[] {null, null, null, null};
		scores = new int[] {-1,-1,-1,-1};

		// Initialise cards, empty player slots
		for (int i = 0; i < 4; i++) {
			cards[i] = rand.nextInt(13) + 1;// we don't want 0
			players[i] = EmptyPlayer;
		}
		// Initialise our new player
		players[0] = player;
	}

	/*
	 * Assign a random suit (suits do not have influence on the card's value)
	 */
	public String getFileNameFromCardNumber(int cardNum) {
		String prefix = "";

		if (cardNum < 11 && cardNum > 1) {
			prefix = Integer.toString(cardNum);
		} else if (cardNum == 1) {
			prefix = "ace";
		} else if (cardNum == 11) {
			prefix = "jack";
		} else if (cardNum == 12) {
			prefix = "queen";
		} else if (cardNum == 13) {
			prefix = "king";
		}

		// Get a random suit
		int s = rand.nextInt(4);

		// build filename
		return "png/" + prefix + "_of_" + suits[s] + ".png";
	}
	
	public boolean haveAllPlayersAnswered() {
		for(int i = 0; i < getNumberOfPlayers(); i++) {
			if (answers[i] == null)
				return false; // false if any of them are empty
		}
		return true;
	}

	public boolean isTherASpareSlot() {
		for (int i = 0; i < 4; i++) {
			if (players[i] == EmptyPlayer) {
				return true;
			}
		}
		return false;
	}

	public void addPlayer(Player p) {
		for (int i = 0; i < 4; i++) {
			if (players[i] == EmptyPlayer) {
				players[i] = p;
				return; // Only do this once
			}
		}
	}
	
	/*
	 * Note: this is the CALCUALTED answer, not the original input.
	 */
	public void setAnswer(int slot, String answer) {
		answers[slot] = answer;
	}

	public int[] getCards() {
		return cards;
	}

	public String getCardsString(String delimiter) {
		String str = "";
		for (int i = 0; i < 4; i++) {
			// append each card value
			str = str + cards[i] + delimiter;
		}
		// Remove last character, which will be an extra delimeter
		str = str.substring(0, str.length() - 1);
		return str;
	}

	public void setCards(String[] cardsPass) {
		for (int i = 0; i < 4; i++) {
			cards[i] = Integer.parseInt((cardsPass[i]));
		}
	}

	public Player[] getPlayers() {
		return players;
	}

	public String getPlayersString(String delimiter) {
		String str = "";
		for (int i = 0; i < 4; i++) {
			if (players[i] != EmptyPlayer) {
				// append each player Id
				str = str + players[i].id + "-" + players[i].name + delimiter;
			}
		}

		// if there are valid players
		if (str != "") {
			// Remove last character, which will be an extra delimeter
			str = str.substring(0, str.length() - 1);
		}
		return str;
	}
	
	public String getPlayerIdsString(String delimiter) {
		String str = "";
		for (int i = 0; i < 4; i++) {
			if (players[i] != EmptyPlayer) {
				// append each player Id
				str = str + players[i].id + delimiter;
			}
		}

		// if there are valid players
		if (str != "") {
			// Remove last character, which will be an extra delimeter
			str = str.substring(0, str.length() - 1);
		}
		return str;
	}

	public void setPlayer(int idx, Player ply) {
		players[idx] = ply;
	}

	public void printPlayers() {
		System.out.println("Players currently in game: ");
		for (int i = 0; i < 4; i++) {
			if (players[i] != EmptyPlayer) // Only print valid players
				System.out.println(players[i].name);
		}
	}

	public void setId(String idPass) {
		id = idPass;
	}

	public String getId() {
		return id;
	}

	public int getNumberOfPlayers() {
		int n = 0;
		for (int i = 0; i < 4; i++) {
			if (players[i] != EmptyPlayer) {
				n++;
			}
		}
		return n;
	}

	public void initialiseId(String idPass) {
		id = idPass;
	}
	
	// Check if this card is actually in play
	public boolean isCardValid(String value) {
		// Convert JQK to int input
		if (value.equals("J"))
			value = "11";
		else if (value.equals("Q"))
			value = "12";
		else if (value.equals("K"))
			value = "13";
		else if (value.equals("A"))
			value = "1";
		

		int parsed;
		try {
			parsed = Integer.parseInt(value);

			// Check against all cards, if any match, then we're all G
			for (int i = 0; i < 4; i++) {
				if (parsed == getCards()[i]) {
					return true;
				}
			}
		} catch (Exception e) {
			System.out.println(e.getStackTrace());
		}
		return false;

	}
	
	/*
	private String convertStringToInt(String inputArr) {
		if (inputArr.equals("J"))
			return "11";
		else if (inputArr.equals("Q"))
			return "12";
		else if (inputArr.equals("K"))
			return "13";
		else if (inputArr.equals("A"))
			return "1";
		return null;
	}*/
	
	public String calculateGameInput(String input) {
		input = input.trim();
		input = input.toUpperCase();

		// Split apart all the elements that were used
		String[] operands = input.split("[-+*/^()]");
		for (int i = 0; i < operands.length; i++) {

			// This can happen with double parens, for example
			// So, if we're actually ealing with a used value
			if (!operands[i].equals("") && operands[i] != null) {

				// if the value is not in the game
				if (!isCardValid(operands[i])) {
					return "Card: [" + operands[i] + "] is not in play!";
				}
			}
		}

		// At this point, we are happy that the values used are actually available in
		// this game
		char[] inChar = input.toCharArray();

		// Convert to string array
		String[] inputArr = new String[input.length()];
		for (int i = 0; i < input.length(); i++) {
			inputArr[i] = Character.toString(inChar[i]);

			// Convert JQKA to int input
			if (inputArr[i].equals("J"))
				inputArr[i] = "11";
			else if (inputArr[i].equals("Q"))
				inputArr[i] = "12";
			else if (inputArr[i].equals("K"))
				inputArr[i] = "13";
			else if (inputArr[i].equals("A"))
				inputArr[i] = "1";

		}
		// And now we have a string which equals the user input with the letters
		// replaced
		input = String.join("", inputArr);

		Object result;
		try {
			result = engine.eval(input);
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			result = "Failed to parse, try using parentheses.";
		}

		if (result == null) {
			return "Enter your answer! (use j, q, and k for face cards)";
		}
		return ""+result;
	}
	
	public String[] getWinners() {
		calculatePlayerScores();
		int min = 99999999;
		String[] winners = new String[] {null,null,null,null};
		int nWinners = 0;
		for (int i = 0; i < getNumberOfPlayers(); i++) {
			if (scores[i] < min) {
				min = scores[i];
				winners[i] = players[i].getId();
				
				// Negate previous winners, we have a new leader
				for (int j = 0; j < i; j++) {
					winners[j] = null;
				}
				nWinners = 1;
			}
			// If the score is tied with the winning score
			else if (scores[i] == min) {
				winners[i] = players[i].getId();
				nWinners++;
			}
		}
		
		// Dont return the nulls, just an array of the actual winners
		String[] winnersPass = new String[nWinners];
		for (int i = 0; i < getNumberOfPlayers(); i++) {
			if (winners[i] != null) {
				for (int j = 0; j < nWinners; j++) {
					if (winnersPass[j] == null) {
						winnersPass[j] = winners[i];
						break;
					}
				}
			}
		}
		
		return winnersPass;
	}
	
	private void calculatePlayerScores() {
		for (int i = 0; i < getNumberOfPlayers(); i++) {
			// Calculate the game input, then parse as an int
			if (answers[i] != null) {				
				int result = Integer.parseInt(answers[i]);
				scores[i] = Math.abs(24 - result);
			}
			else {
				scores[i] = 999999;
			}
		}
	}
}
