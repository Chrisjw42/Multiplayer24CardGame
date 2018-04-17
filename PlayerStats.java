
public class PlayerStats {
	public String id;
	public String name;
	public int nGames;
	public int nWins;
	public float winPercentage;
	
	public PlayerStats() {
		
	}
	
	public String toString() {
		return "name: " + name + ", nGames: "+ nGames + " winPc: " + winPercentage;
	}
}
