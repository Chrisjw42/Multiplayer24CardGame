
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
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

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
	private JPanel panel1Usr;
	private JPanel panel2Game;
	private JPanel panel3Ldr;
	private JPanel panel4Logout;
	private JTabbedPane mainTabs;

	// USER
	private JLabel lblUserName;
	private JLabel lblUserStats;
	private JLabel lblUserRank;

	// GAME
	private JButton btnNewGame;
	private JPanel gamePanel;
	private JPanel gamePanelInput;
	private JPanel gamePanelOptions;
	private JButton gameExit;
	private JTextField gameInput;
	private JLabel gameInputFeedback;
	private JLabel gameInputInstructions;
	private JButton btnGameSubmit;

	private JLabel[] gameCardLabels;
	private ImageIcon[] gameCardImages;

	// LEADERBOARD
	private JTable tblLeader;

	// LOGOUT
	private JButton btnLogout;

	// Pass the client to this window's constructor
	public MainWindow(LoginClient clientPass, Player playerPass) {
		player = playerPass;
		client = clientPass;
	}

	public void run() {
		rand = new Random();

		main = new JFrame("The Game");
		main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		panel1Usr = new JPanel();
		panel2Game = new JPanel();
		panel3Ldr = new JPanel();
		panel4Logout = new JPanel();

		panel1Usr.setBorder(new EmptyBorder(20, 20, 20, 20));
		panel2Game.setBorder(new EmptyBorder(20, 20, 20, 20));
		panel3Ldr.setBorder(new EmptyBorder(20, 20, 20, 20));
		panel4Logout.setBorder(new EmptyBorder(20, 20, 20, 20));

		// USER
		BoxLayout boxUsr = new BoxLayout(panel1Usr, BoxLayout.Y_AXIS);
		panel1Usr.setLayout(boxUsr);
		lblUserName = new JLabel("<html><h2>" + player.name + "</h2><html>");
		lblUserStats = new JLabel(String.format("<html>Number of wins: %d<br />" + "Number of Games: %d<br />"
				+ "Average time to win: %f seconds</html>", 10, 20, 12.5));
		lblUserRank = new JLabel("<html><h3>Rank: #42</h3></html>");

		panel1Usr.add(lblUserName);
		panel1Usr.add(lblUserStats);
		panel1Usr.add(lblUserRank);

		// GAME
		BoxLayout boxGame = new BoxLayout(panel2Game, BoxLayout.Y_AXIS);
		panel2Game.setLayout(boxGame);

		btnNewGame = new JButton("New Game");
		btnNewGame.addActionListener(this);

		panel2Game.add(btnNewGame);

		// LEADERBOARD
		GridLayout pnlLayout = new GridLayout();
		panel3Ldr.setLayout(pnlLayout);
		String[] colNames = { "Rank", "Player", "Games Won", "Games Played", "Avg. Winning Time" };
		Object[][] data = { { 1, "test", 15, 20, 7.01 }, { 1, "test", 15, 20, 7.01 }, { 1, "test", 15, 20, 7.01 },
				{ 1, "test", 15, 20, 7.01 }, { 1, "test", 15, 20, 7.01 }, { 1, "test", 15, 20, 7.01 },
				{ 1, "test", 15, 20, 7.01 }, { 1, "test", 15, 20, 7.01 } };

		tblLeader = new JTable(data, colNames);
		panel3Ldr.add(tblLeader);

		// LOGOUT
		BoxLayout boxLogout = new BoxLayout(panel4Logout, BoxLayout.Y_AXIS);
		panel4Logout.setLayout(boxLogout);
		btnLogout = new JButton("Log Out");
		btnLogout.addActionListener(this);

		panel4Logout.add(btnLogout);

		mainTabs = new JTabbedPane();
		mainTabs.addTab("User Profile", panel1Usr);
		mainTabs.addTab("Play Game", panel2Game);
		mainTabs.addTab("Leaderboard", panel3Ldr);
		mainTabs.addTab("Log Out", panel4Logout);

		main.add(mainTabs);

		mainTabs.setPreferredSize(new Dimension(800, 500));
		main.setPreferredSize(new Dimension(800, 500));
		main.setResizable(false);

		main.pack();
		main.setLocationRelativeTo(null);
		main.setVisible(true);
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
		// Disable newgame button
		btnNewGame.setVisible(false);

		// get a game object from the server
		game = getGame();

		// Set up game UI elements
		gameCardLabels = new JLabel[4];
		gameCardImages = new ImageIcon[4];
		gameInputFeedback = new JLabel("=  ");
		gamePanel = new JPanel();
		gamePanelOptions = new JPanel();
		gamePanelInput = new JPanel();
		gameInputInstructions = new JLabel("Your Answer: ");

		gamePanel.setPreferredSize(new Dimension(400, 400));
		gamePanelOptions.setPreferredSize(new Dimension(400, 45));
		gamePanelOptions.setBorder(BorderFactory.createEtchedBorder(1));
		gamePanelOptions.setLayout(new FlowLayout(FlowLayout.RIGHT));
		gamePanelOptions.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		gamePanelInput.setPreferredSize(new Dimension(400, 150));
		gamePanelInput.setBorder(BorderFactory.createEtchedBorder(1));
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
			gamePanel.add(gameCardLabels[i]);

			String filename = game.getFileNameFromCardNumber(game.getCards()[i]);
			gameCardImages[i] = resizeImage(filename, lblSize.width, lblSize.height);
			gameCardLabels[i].setIcon(gameCardImages[i]);

			gameCardLabels[i].setVisible(true);

			Border etched = BorderFactory.createEtchedBorder(1);
			gameCardLabels[i].setBorder(etched);
		}

		// Adding the elements to the panels

		gamePanelInput.add(gameInputInstructions);
		gamePanelInput.add(gameInput);
		gamePanelInput.add(gameInputFeedback);
		gamePanelInput.add(btnGameSubmit);
		gamePanelOptions.add(gameExit);
		panel2Game.add(gamePanelOptions);
		panel2Game.add(gamePanel);
		panel2Game.add(gamePanelInput);
		gamePanelOptions.setVisible(true);
		gamePanel.setVisible(true);
		gamePanelInput.setVisible(true);
		gameInputFeedback.setOpaque(true);
		gameInputFeedback.setVisible(true);
		
		btnGameSubmit.addActionListener(this);
		
		// TODO: Fix the alignment of the label
		gameInput.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				JTextField textField = (JTextField) e.getSource();
				String text = textField.getText();
				gameInputFeedback.setText(game.calculateGameInput(text));
			}
		});
	}

	public void quitGame() {

		gamePanelOptions.removeAll();
		gamePanelInput.removeAll();
		gamePanel.removeAll();
		panel2Game.remove(gamePanelOptions);
		panel2Game.remove(gamePanelInput);
		panel2Game.remove(gamePanel);
		panel2Game.repaint();

		game = null;

		// TODO: Notify server?

		btnNewGame.setVisible(true);
	}

	public Game getGame() {
		// TESTING
		//return client.dbConn.getTestGame();

		try {
			
			Game serverResponse = client.jmsClient.getGame(player);
			
			return serverResponse;
		} catch (JMSException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public void awaitResult() {
		// TODO: Store this globally for use in UI
		// TODO: first evoke a local UI method to show "waiting for other player's results"
		GameResult gR = client.jmsClient.awaitResult(player, game);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getActionCommand() == "Log Out") {
			client.logoutAndReset();
		} else if (arg0.getActionCommand() == "New Game") {
			
			game = getGame();
			if (game != null) {
				beginGame();
			} else {
				// TODO: "sorry try again" message
			}
		} else if (arg0.getActionCommand() == "Quit Game") {
			
			quitGame();
		} else if (arg0.getActionCommand() == "Submit Answer") {
			
			String answer = gameInputFeedback.getText();
			try {
				client.jmsClient.submitGameAnswer(player, game, answer);
				System.out.println("Answer Submitted.");
				awaitResult();
				
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
	}
}
