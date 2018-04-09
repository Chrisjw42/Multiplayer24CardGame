
public class GameResult {

	private String[] winners;
	private Game game;
	
	public GameResult(Game gamePass, String[] winnersPass) {
		game = gamePass;
		winners = winnersPass;
	}
	
	/*
	public GameResult(Game gamePass) {
		game = gamePass;
	}*/
	
	public String[] getWinners() {
		return winners;
	}
	
	public String getWinnersString() {
		String res = "";
		
		for (int i = 0; i < winners.length; i++) {
			res = res + winners[i];
			
			// If this isn't the last winner
			if (winners.length - i > 1) {
				res = res + ",";
			}
		}
		
		return res;
	}
	
	public Game getGame() {
		return game;
	}
	
	public void addWinner(String winnerId) {
		for(int i = 0; i < 4; i++) {
			if(winners[i] == null) {
				winners[i] = winnerId;
			}
		}
	}
}
