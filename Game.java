import java.util.Random;

public class Game {
	private String id;
	private Player[] players;
	private int[] cards;
	private Random rand;
	private String[] suits;
	
	private Player EmptyPlayer;
	
	// new game requires minimum 1 player
	public Game(Player player) {
		rand = new Random();
		InitialiseId();
		// Dummy player
		EmptyPlayer = new Player("emptyplayer", "-1");
		
		// TODO: pass players to constructor
		suits = new String[] {"spades", "clubs", "diamonds", "hearts"};
		cards = new int[4];
		players = new Player[4];
		
		// Initialise cards, empty player slots
		for(int i = 0; i < 4; i++) {
			cards[i] = rand.nextInt(13) + 1;// we don't want 0
			players[i] = EmptyPlayer;
		}
		// Initialise our new playet
		players[0] = player;
	}
	
	/*
	 * Assign a random suit (suits do not have influence on the card's value)
	 */
	public String GetFileNameFromCardNumber(int cardNum) {		
		String prefix = "";
		
		if (cardNum < 11 && cardNum > 1) {
			prefix = Integer.toString(cardNum);
		}
		else if (cardNum == 1) {
			prefix = "ace";
		}
		else if (cardNum == 11) {
			prefix = "jack";
		}
		else if (cardNum == 12) {
			prefix = "queen";
		}
		else if (cardNum == 13){
			prefix = "king";
		}
		
		// Get a random suit
		int s = rand.nextInt(4);
			
		// build filename
		return "png/" + prefix + "_of_" + suits[s] + ".png";
	}
	
	public boolean SpareSlot() {
		for(int i = 0; i < 4; i++) {
			if(players[i] == EmptyPlayer) {
				return true;
			}
		}
		return false;
	}
	
	public void AddPlayer(Player p) {
		for(int i = 0; i < 4; i++) {
			if(players[i] == EmptyPlayer) {
				players[i] = p;
				return; // Only do this once
			}
		}
	}
	
	public int[] GetCards() {
		return cards;
	}
	
	public String GetCardsString(String delimiter) {
		String str = "";
		for (int i =0; i< 4; i++) {
			// append each card value
			str = str+cards[i]+delimiter;
		}
		// Remove last character, which will be an extra delimeter
		str = str.substring(0, str.length() - 1);
		return str;
	}
	
	public void SetCards(String[] cardsPass) {
		for (int i = 0; i < 4; i++) {
			cards[i] = Integer.parseInt((cardsPass[i]));
		}
	}
	
	public Player[] GetPlayers() {
		return players;
	}
	
	public String GetPlayersString(String delimiter){
		String str = "";
		for (int i =0; i< 4; i++) {
			if (players[i] != EmptyPlayer) {
				// append each player Id
				str = str+players[i].id+"-"+players[i].name+delimiter;
			}
		}
		
		// if there are valid players
		if (str != "") {
			// Remove last character, which will be an extra delimeter
			str = str.substring(0, str.length() - 1);
		}
		return str;
	}
	
	public void SetPlayer(int idx, Player ply) {
		players[idx] = ply;
	}
	
	public void PrintPlayers() {
		System.out.println("Players currently in game: ");
		for (int i = 0; i< 4; i++) {
			if (players[i] != EmptyPlayer) // Only print valid players
				System.out.println(players[i].name);
		}
	}
	
	public void SetId(String idPass) {
		id = idPass;	
	}
	
	public String GetId() {
		return id;
	}
	
	public int GetNumberOfPlayers() {
		int n = 0;
		for (int i =0; i< 4; i++) {
			if (players[i] != EmptyPlayer) {
				n++;
			}
		}
		return n;
	}
	
	public void InitialiseId() {
		// TODO: This lel
		id = ""+rand.nextInt(999999999);
	}
	
}
