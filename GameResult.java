
public class GameResult {

	private String[] winners;
	private Game game;
	
	public GameResult(Game gamePass) {
		game = gamePass;
		winners = game.getWinners();
	}
	
	public String[] getWinners() {
		return winners;
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
