
public class PotentialGame {
	public Game game;
	public int waitingTime;
	
	public PotentialGame(Player player) {
		waitingTime = 0;
		game = new Game(player); // Pretty sure this is fixed, just need to test.
	}	
	
	public void IncrementWaitingTime(int i) {
		waitingTime += i;
	}
	
	public void ResetWaitingTime() {
		waitingTime = 0;
	}
}
