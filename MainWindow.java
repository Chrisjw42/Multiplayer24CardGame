
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.jms.JMSException;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

public class MainWindow implements Runnable, ActionListener {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new LoginClient(args[0]));
	}

	Random rand;


	private LoginClient client;
	private Game game;
	private Player player;

	// UI Elements
	JFrame main;
	private JPanel pnl1Usr;
	private JPanel pnl2Game;
	private JPanel pnl3Ldr;
	private JPanel pnl4Logout;
	private JTabbedPane mainTabs;

	// USER
	private JLabel lblUserName;
	private JLabel lblUserStats;
	private JLabel lblUserRank;

	// GAME
	private JButton btnNewGame;
	private JPanel pnlGame;
	private JPanel pnlGameInput;
	private JPanel pnlGameOptions;
	private JPanel pnlPlayers;
	private JButton gameExit;
	private JTextField gameInput;
	private JLabel gameInputFeedback;
	private JLabel gameInputInstructions;
	private JButton btnGameSubmit;

	private JLabel[] gameCardLabels;
	private ImageIcon[] gameCardImages;
	private JLabel[] gamePlayerLabels;
	
	// LEADERBOARD
	private JTable tblLeader;

	// LOGOUT
	private JButton btnLogout;
	
	// GAMEOVER
	private JPanel pnlGameOver;
	private JLabel lblGameOverText;

	// Pass the client to this window's constructor
	public MainWindow(LoginClient clientPass, Player playerPass) {
		player = playerPass;
		client = clientPass;
	}

	public void run() {
		rand = new Random();

		main = new JFrame("The Game");
		main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		pnl1Usr = new JPanel();
		pnl2Game = new JPanel();
		pnl3Ldr = new JPanel();
		pnl4Logout = new JPanel();

		pnl1Usr.setBorder(new EmptyBorder(20, 20, 20, 20));
		pnl2Game.setBorder(new EmptyBorder(20, 20, 20, 20));
		pnl3Ldr.setBorder(new EmptyBorder(20, 20, 20, 20));
		pnl4Logout.setBorder(new EmptyBorder(20, 20, 20, 20));

		// USER
		BoxLayout boxUsr = new BoxLayout(pnl1Usr, BoxLayout.Y_AXIS);
		pnl1Usr.setLayout(boxUsr);
		lblUserName = new JLabel("<html><h2>" + player.name + "</h2><html>");
		
		int[] pStats = client.dbConn.getPlayerStats(player.id);
		
		if(pStats == null || pStats[0] == 0) {
			lblUserStats = new JLabel(String.format("<html>Number of Game: %d<br />" + "Number of Wins: %d<br />"
					+ "Win Percentage: %f%%</html>", 0,0,0f));
		
		}
		else {
			lblUserStats = new JLabel(String.format("<html>Number of Game: %d<br />" + "Number of Wins: %d<br />"
					+ "Win Percentage: %f%%</html>", pStats[0], pStats[1], (float)((float)pStats[1]/(float)pStats[0])));
		}
		int pRank = client.dbConn.getSpecificPlayerRank(player.name);
		if (pRank == -1) {
			lblUserRank = new JLabel("<html><h3>Rank: Not available, play a game!</h3></html>");			
		}
		else {
			lblUserRank = new JLabel("<html><h3>Rank: "+pRank+"</h3></html>");
		}
		pnl1Usr.add(lblUserName);
		pnl1Usr.add(lblUserStats);
		pnl1Usr.add(lblUserRank);

		// GAME
		BoxLayout boxGame = new BoxLayout(pnl2Game, BoxLayout.Y_AXIS);
		pnl2Game.setLayout(boxGame);

		btnNewGame = new JButton("New Game");
		btnNewGame.addActionListener(this);

		pnl2Game.add(btnNewGame);

		// LEADERBOARD
		updateLeaderBoard();
		

		// LOGOUT
		BoxLayout boxLogout = new BoxLayout(pnl4Logout, BoxLayout.Y_AXIS);
		pnl4Logout.setLayout(boxLogout);
		btnLogout = new JButton("Log Out");
		btnLogout.addActionListener(this);

		pnl4Logout.add(btnLogout);

		mainTabs = new JTabbedPane();
		mainTabs.addTab("User Profile", pnl1Usr);
		mainTabs.addTab("Play Game", pnl2Game);
		mainTabs.addTab("Leaderboard", pnl3Ldr);
		mainTabs.addTab("Log Out", pnl4Logout);

		main.add(mainTabs);

		mainTabs.setPreferredSize(new Dimension(800, 500));
		main.setPreferredSize(new Dimension(800, 500));
		main.setResizable(false);

		main.pack();
		main.setLocationRelativeTo(null);
		main.setVisible(true);
	}
	
	private void updateLeaderBoard() {
		pnl3Ldr.removeAll();
		GridLayout pnlLayout = new GridLayout();
		pnl3Ldr.setLayout(pnlLayout);
		String[] colNames = { "Rank", "Player", "Games Played", "Games Won", "Win %" };
		PlayerStats[] pRankings = client.dbConn.getRankedPlayerStats();
		Object[][] data = new Object[pRankings.length][];
		
		for (int i = 0; i < pRankings.length; i++) {
			data[i] = new Object[]{i + 1, pRankings[i].name, pRankings[i].nGames, pRankings[i].nWins, ""+pRankings[i].winPercentage*100+"%"};
		}
		tblLeader = new JTable(data, colNames);
		pnl3Ldr.add(new JScrollPane(tblLeader));
	}

	public ImageIcon resizeImage(String filePath, int width, int height) {

		// Read image as a bufferedImage
		BufferedImage img = null;
		try {
			System.out.println(filePath);
			img = ImageIO.read(new File(filePath));
		} catch (IOException e) {
			e.printStackTrace();
		}

		Image dimg = img.getScaledInstance(width - 10, height - 10, Image.SCALE_AREA_AVERAGING);

		ImageIcon finalImg = new ImageIcon(dimg);

		return finalImg;
	}

	public void beginGame() {
		// Get rid of "you won" UI elements.
		
		purgeGameUIElements(false);
		
		btnNewGame.setVisible(false);

		System.out.println("Finding a game, please wait...");
		JLabel loading = new JLabel("Finding a game, please wait...");
		pnl2Game.add(loading);
		loading.setVisible(true);
		pnl2Game.repaint();
		
		// get a game object from the server
		game = getGame();

		pnl2Game.remove(loading);
		loading = null;
		
		// Set up game UI elements
		gameCardLabels = new JLabel[4];
		gameCardImages = new ImageIcon[4];
		gamePlayerLabels = new JLabel[4];
		gameInputFeedback = new JLabel("=  ");
		pnlGame = new JPanel();
		pnlGameOptions = new JPanel();
		pnlGameInput = new JPanel();
		pnlPlayers = new JPanel();
		gameInputInstructions = new JLabel("Your Answer: ");

		pnlGame.setPreferredSize(new Dimension(400, 400));
		pnlGameOptions.setPreferredSize(new Dimension(400, 45));
		//pnlGameOptions.setBorder(BorderFactory.createEtchedBorder(1));
		pnlGameOptions.setLayout(new FlowLayout(FlowLayout.RIGHT));
		pnlGameOptions.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		pnlGameInput.setPreferredSize(new Dimension(400, 150));
		//pnlGameInput.setBorder(BorderFactory.createEtchedBorder(1));
		pnlPlayers.setPreferredSize(new Dimension(50,400));
		pnlPlayers.setLayout(new FlowLayout());
		gameExit = new JButton("Quit Game");
		gameExit.addActionListener(this);
		gameInput = new JTextField(20);
		gameInput.setName("Game Input");
		btnGameSubmit = new JButton("Submit Answer");

		// Limit the label size to avoid dogey resizing when the text feedback changes
		Dimension d = new Dimension(300, 20);
		gameInputFeedback.setPreferredSize(d);
		gameInputFeedback.setMinimumSize(d);
		gameInputFeedback.setMaximumSize(d);

		// How much to scale the card size
		float scaleFactor = 0.75f;
		Dimension lblSize = new Dimension(Math.round(200 * scaleFactor), Math.round(300 * scaleFactor));

		// Initialise each card
		for (int i = 0; i < 4; i++) {
			gameCardLabels[i] = new JLabel();
			gameCardLabels[i].setPreferredSize(lblSize);
			gameCardLabels[i].setBackground(new Color(255, 255, 255));
			pnlGame.add(gameCardLabels[i]);

			String filename = game.getFileNameFromCardNumber(game.getCards()[i]);
			gameCardImages[i] = resizeImage(filename, lblSize.width, lblSize.height);
			gameCardLabels[i].setIcon(gameCardImages[i]);

			gameCardLabels[i].setVisible(true);

			Border etched = BorderFactory.createEtchedBorder(1);
			gameCardLabels[i].setBorder(etched);
		}
		
		for (int p = 0; p < game.getNumberOfPlayers(); p++) {
			gamePlayerLabels[p] = new JLabel(); 
			Border b = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
			Border margin = new EmptyBorder(10,10,10,10);
			Border c = new CompoundBorder(b, margin);
			gamePlayerLabels[p].setBorder(c);
			gamePlayerLabels[p].setPreferredSize(new Dimension(150,90));
			String txt = "<HTML><h3>Player "+(p+1)+": "+game.getPlayers()[p].name+"</h3>";
			
			int r = client.dbConn.getSpecificPlayerRank(game.getPlayers()[p].name);
			
			// Player might not be ranked yet
			if (r != -1)
				txt = txt+"<h4>Rank: "+r+"</h4></HTML>";
			else 
				txt = txt+"<h4>Unranked! </h4></HTML>";
			
			gamePlayerLabels[p].setText(txt);
			pnlPlayers.add(gamePlayerLabels[p]);
			
			if (game.getPlayers()[p].id.equals(player.id)) {
				gamePlayerLabels[p].setOpaque(true);
				gamePlayerLabels[p].setBackground(new Color(150,234,150));
			}
		}

		// Adding the elements to the panels
		pnlGameInput.add(gameInputInstructions);
		pnlGameInput.add(gameInput);
		pnlGameInput.add(gameInputFeedback);
		pnlGameInput.add(btnGameSubmit);
		btnGameSubmit.addActionListener(this);
		pnlGameOptions.add(gameExit);		
		
		pnl2Game.add(pnlGameOptions);
		pnl2Game.add(pnlGame);
		pnl2Game.add(pnlGameInput);
		pnl2Game.add(pnlPlayers);

		pnlGameOptions.setVisible(true);
		pnlGame.setVisible(true);
		pnlGameInput.setVisible(true);
		pnlPlayers.setVisible(true);
		gameInputFeedback.setOpaque(true);
		gameInputFeedback.setVisible(true);
		
		// TODO: Fix the alignment of the label
		gameInput.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String text = textField.getText();
				gameInputFeedback.setText("= "+game.calculateGameInput(text));
			}
		});
		
		pnl2Game.repaint();
	}

	public void purgeGameUIElements(boolean earlyExit) {
		pnl2Game.removeAll();
		pnl2Game.repaint();
		game = null;

		if (earlyExit) {
			// TODO: Notify server?			
		}
	}

	public Game getGame() {
		// TESTING
		//return client.dbConn.getTestGame();

		try {
			System.out.println("Initiating JMS client");
			Game serverResponse = client.jmsClient.getGame(player);
			System.out.println("JMS client response received");
			return serverResponse;
		} catch (JMSException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public boolean getResult() {
		// TODO: Store this globally for use in UI?
		// TODO: first evoke a local UI method to show "waiting for other player's results"
		GameResult gR = client.jmsClient.awaitResult(player, game);
		String[] winners = gR.getWinners();
		
		for (int i = 0; i < winners.length; i++) {
			
			if (winners[i] != null) {
				System.out.println(winners[i]);
				if(player.getId().equals(gR.getWinners()[i])) {
					return true;
				}
			}
		}
		return false;
	}
	
	private void endGame(boolean didPlayerWin, String answer) {
		// hide all game elements, set null 
		purgeGameUIElements(false);
		client.jmsClient.game = null;
		
		// Display game results
		pnlGameOver = new JPanel();
		pnlGameOver.setBorder(new EmptyBorder(20,20,20,20));
		BoxLayout bx = new BoxLayout(pnlGameOver, BoxLayout.Y_AXIS);
		pnlGameOver.setLayout(bx);
		
		lblGameOverText = new JLabel();
		String txt = "<html><h1>You ";
		if (didPlayerWin)
			txt = txt+"Won!</h1>";
		else
			txt = txt+"Lost!</h1>";
		txt = txt+"<h3>Answer: "+answer+"</h3></html>";
		lblGameOverText.setText(txt);
		
		btnNewGame = new JButton("New Game");
		btnNewGame.addActionListener(this);

		// Add to panel, set visible
		pnlGameOver.add(lblGameOverText);
		pnlGameOver.add(btnNewGame);
		pnl2Game.add(pnlGameOver);
		pnl2Game.add(btnNewGame);
		updateLeaderBoard();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getActionCommand() == "Log Out") {
			client.logoutAndReset();
		} else if (arg0.getActionCommand() == "New Game") {
			
			// If we're looking at the gameOver results, remove those.
			if(pnlGameOver != null) {
				pnlGameOver.removeAll();
				pnlGameOver = null;
			}		
			
			SwingWorker myWorker= new SwingWorker<Void, String>() {
			    @Override
			    protected Void doInBackground() throws Exception {
			    	// TODO: Some kind of "try again" situation
			    	beginGame();
					return null;
			    }
			};
			myWorker.execute();
			
		} else if (arg0.getActionCommand() == "Quit Game") {
			
			// true as in, game is not actually over
			purgeGameUIElements(true);
		} else if (arg0.getActionCommand() == "Submit Answer") {
			
			SwingWorker myWorker= new SwingWorker<Void, String>() {
			    @Override
			    protected Void doInBackground() throws Exception {
			    	String answer = gameInput.getText();
					answer = game.calculateGameInput(answer);
					try {
						btnGameSubmit.setVisible(false);
						pnlGameInput.add(new JLabel("Awaiting Results..."));
						client.jmsClient.submitGameAnswer(player, game, answer);
						System.out.println("Answer Submitted.");
						endGame(getResult(), gameInput.getText());
						
					} catch (JMSException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return null;
			    }
			};
			myWorker.execute();
		}		
	}
}
