import java.util.Enumeration;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import com.sun.messaging.jmq.jmsclient.TextMessageImpl;

public class JmsClient {

	private String host;
	private Context jndiContext;
	private ConnectionFactory connectionFactory;
	private Connection conn;
	private Session sess;
	private LinkedList<String> localActiveGames;

	private Queue playerQueue;
	private Queue gameQueue;
	private Queue activeGameQueue;
	private Queue gameInputQueue;
	private Queue gameResultQueue;
	private MessageProducer playerQueueSender;
	private MessageProducer gameInputSender;
	private MessageConsumer queueReceiver;

	public Game game;
	
	public JmsClient(String host) { // TODO: consider passing Player object reference
		this.host = host;
		localActiveGames = new LinkedList<String>();
	}

	public boolean initialise() {
		// Access JNDI
		try {
			game = null;
			createJNDIContext();
			// Lookup JMS resources
			lookupConnectionFactory();
			lookupQueues();

			// Create connection->session->sender
			createConnection();
			createSession();
			return true;
		} catch (NamingException | JMSException e) {
			// TODO Auto-generated catch block
			System.out.println("Failed to create connection to Glassfish JMS instance");
			e.printStackTrace();
			return false;
		}
	}

	/*
	 * Append a playerID to the game list
	 */
	public void enqueuePlayer(Player player) throws JMSException {
		createPlayerQueueSender();
		TextMessage msg = sess.createTextMessage();
		msg.setText(player.name + "," + player.id);
		playerQueueSender.send(msg);
		// send non-text control message to end
		// playerQueueSender.send(sess.createMessage());
	}
	
	/*
	 * Append a playerID to the game list
	 */
	public void submitGameAnswer(Player p, Game g, String answer) throws JMSException {
		
		// Make sure player is in game. 
		Player[] plys = g.getPlayers();
		boolean fraud = true;
		for (int i = 0; i < 4; i++) {
			if (plys[i].id.equals(p.id)) {
				fraud = false;
			}
		}
		if (fraud == true) {
			System.err.println("Error, trying to submit answer for game you are not partaking in.");
			return;
		}
		
		// good to go
		createGameInputSender();
		
		TextMessage msg = sess.createTextMessage();
		msg.setText(p.id + "," + g.getId() + "," + answer);
		System.out.println("uploading gameAnswer: " + msg.getText());
		gameInputSender.send(msg);
		// send non-text control message to end
		// playerQueueSender.send(sess.createMessage());
	}

	public Game decodeGameMessage(String gameMessage) {
		System.out.println("Decoding: " + gameMessage);
		String thisGame = gameMessage;
		// Decode the message
		String[] values = thisGame.split(",");
		String gameId = values[0];
		String[] playerRaw = values[1].split("&");
		String[] cards = values[2].split("&");

		// Each row is a player, each of the two cells are id and name respectively.
		String[][] playerInfo = new String[playerRaw.length][2];
		Player[] players = new Player[playerRaw.length];

		// Input format is "id-name"
		for (int i = 0; i < playerRaw.length; i++) {
			playerInfo[i] = playerRaw[i].split("-");
			players[i] = new Player(playerInfo[i][1], playerInfo[i][0]);
			System.out.println("Player in this game: " + playerInfo[i][1].toString());
		}

		Game g = new Game(players[0]);
		g.setId(gameId);
		g.setCards(cards);

		// j = 1, because the first player was already passed
		for (int j = 1; j < playerRaw.length; j++) {
			g.setPlayer(j, players[j]);
		}

		return g;
	}

	public Game findActiveGameWithPlayer(Player player) throws JMSException {
		
		// TODO: utilise the general method "observerqueue" here
		// Read queue without consuming messages
		QueueBrowser queueBrowser = sess.createBrowser(activeGameQueue);
		Enumeration msgs = queueBrowser.getEnumeration();

		// Get every game in the list
		while (msgs.hasMoreElements()) {
			String thisGame;
			try {
				thisGame = ((TextMessageImpl) msgs.nextElement()).getText();
			} catch (ClassCastException e) {
				System.err.println("Tried to parse a message that was not text-based.");
				thisGame = null;
				continue; // Skip this iteration
			}

			//System.out.println(thisGame);

			Game g = decodeGameMessage(thisGame);

			// For this game, we check if any of the players match the player we are
			// searching for
			for (int i = 0; i < 4; i++) {
				//System.out.println(g.getPlayers()[i].id);
				if (g.getPlayers()[i].id.equals(player.id)) {
					// ding ding!
					System.out.println("Player: " + player.id + " has found their game, and "
							+ "it is active, and ready to be played.");
					// game is good to go!
					return g;
				}
			}
		}
		return null;
	}

	/*
	 * Get a game
	 */
	public Game getGame(Player player) throws JMSException, InterruptedException {

		if (game == null) {
			// Add player to the list of seeking players
			enqueuePlayer(player);
			System.out.println("GettingsGame for: " + player.name);

			// Every 1 second, try and read available game list
			while (game == null) {
				System.out.println("Checking activeGame list...");
				game = findActiveGameWithPlayer(player);
				TimeUnit.SECONDS.sleep(1);
			}
			
		}
		return game;
	}

	private void createJNDIContext() throws NamingException {
		try {

			System.setProperty("org.omg.CORBA.ORBInitialHost", host);
			System.setProperty("org.omg.CORBA.ORBInitialPort", "3700");

			jndiContext = new InitialContext();

		} catch (NamingException e) {
			System.err.println("Could not create JNDI API context: " + e);
			throw e;
		}
	}

	private void lookupConnectionFactory() throws NamingException {
		try {
			connectionFactory = (ConnectionFactory) jndiContext.lookup("jms/GameConnectionFactory");
		} catch (NamingException e) {
			System.err.println("JNDI API JMS connection factory lookup failed: " + e);
			throw e;
		}
	}

	private void lookupQueues() throws NamingException {

		try {
			playerQueue = (Queue) jndiContext.lookup("jms/PlayerQueue");
			gameQueue = (Queue) jndiContext.lookup("jms/GameQueue");
			gameInputQueue = (Queue) jndiContext.lookup("jms/GameInputQueue");
			activeGameQueue = (Queue) jndiContext.lookup("jms/ActiveGameQueue");
			gameResultQueue = (Queue) jndiContext.lookup("jms/GameResultQueue");
		} catch (NamingException e) {
			System.err.println("JNDI API JMS queue lookup failed: " + e);
			throw e;
		}
	}

	/*
	 * Parametrised, specify the JMS queue to pull from, as well as the local queue
	 * to store results in
	 */
	public void receiveMessages(Queue relevantQueue, LinkedList<String> localQueue) throws JMSException {
		createReceiver(relevantQueue);
		while (true) {
			// The library is designed to hang forever on this until a message is received.
			Message m = queueReceiver.receive(2);// Timeout of x seconds
			if (m != null && m instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) m;
				System.out.println("Received message: " + textMessage.getText());
				localQueue.add(textMessage.getText());

			} else {
				queueReceiver.close();
				break;
			}
		}
	}
	
	public GameResult awaitResult(Player player, Game game) {
		while(true) {
			System.out.println("Awaiting result...");
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			LinkedList<String> gameResultMsgs = null;
			try {
				gameResultMsgs = observeMessages(gameResultQueue);
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// Check every result string
			for (int i = 0; i < gameResultMsgs.size(); i++) {
				System.out.println("Observed GameResult: "+gameResultMsgs.get(i));
				String[] components = gameResultMsgs.get(i).split(",");
				// If the posted result matches this game
				if (components[0].equals(game.getId())) {
					
					// Add the winners from the message to the GameResult
					String[] winners = components[1].split("&");
					GameResult gR = new GameResult(game, winners);
					
					/*
					for(int j = 0; j < winners.length; j++) {
						gR.addWinner(winners[j]);
					}*/
					
					// Only happens once a matching result has been found
					return gR;
				}
			}
		}
	}
	
	
	/*
	 * Read queue messages WITHOUT consuming them. 
	 */
	public LinkedList<String> observeMessages(Queue relevantQueue) throws JMSException {
		// Read queue without consuming messages
		QueueBrowser queueBrowser = sess.createBrowser(relevantQueue);
		Enumeration msgs = queueBrowser.getEnumeration();

		LinkedList<String> msgsReturn = new LinkedList<String>();
		
		// Get every game in the list
		while (msgs.hasMoreElements()) {
			String thisMsg;
			try {
				thisMsg = ((TextMessageImpl) msgs.nextElement()).getText();
				msgsReturn.add(thisMsg);
			} catch (ClassCastException e) {
				System.err.println("Tried to parse a message that was not text-based.");
				thisMsg = null;
				continue; // Skip this iteration
			}
		}
		return msgsReturn;
	}

	private void createReceiver(Queue relevantQueue) throws JMSException {
		try {
			queueReceiver = sess.createConsumer(relevantQueue);
		} catch (JMSException e) {
			System.err.println("Failed to create session: " + e);
			throw e;
		}
	}

	private void createConnection() throws JMSException {
		try {
			conn = connectionFactory.createConnection();
			conn.start();
		} catch (JMSException e) {
			System.err.println("Failed to create connection to JMS provider: " + e);
			throw e;
		}
	}

	private void createSession() throws JMSException {
		try {
			sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
		} catch (JMSException e) {
			System.err.println("Failed to create session: " + e);
			throw e;
		}
	}

	// Was: QueueSender
	private void createPlayerQueueSender() throws JMSException {
		try {
			playerQueueSender = sess.createProducer(playerQueue);
		} catch (JMSException e) {
			System.err.println("Failed to create session: " + e);
			throw e;
		}
	}

	// Was: QueueSender
	private void createGameInputSender() throws JMSException {
		try {
			gameInputSender = sess.createProducer(gameInputQueue);
		} catch (JMSException e) {
			System.err.println("Failed to create session: " + e);
			throw e;
		}
	}

	
	public void close() {
		if (conn != null) {
			try {
				conn.close();
			} catch (JMSException e) {
			}
		}
	}

}
