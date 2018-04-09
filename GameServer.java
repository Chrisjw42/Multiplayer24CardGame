import java.sql.SQLException;
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

public class GameServer {

	private String host;
	private Context jndiContext;
	private ConnectionFactory connectionFactory;
	private Connection conn;
	private Session sess;
	private LinkedList<String> localPlayerQueue;
	private LinkedList<PotentialGame> localPotentialGames;
	private LinkedList<String> localGameInputs;
	private LinkedList<Game> localActiveGames;
	
	private Queue playerQueue;
	private Queue activeGameQueue;
	private Queue gameInputQueue;
	private Queue gameResultQueue;

	private MessageProducer activeGameQueueSender;
	private MessageProducer gameResultQueueSender;
	
	private MessageConsumer queueReceiver;
	
	private DBConnection dbConn;
	
	public static void main(String[] args) {
		String host = "localhost";
		GameServer gameServer = null;

		try {
			gameServer = new GameServer(host);

			/*
			// Call this thread object when the program shuts down (cleanly)
			ShutDownThread sdT = new ShutDownThread(gameServer);
			Runtime.getRuntime().addShutdownHook(sdT);
*/
			
			// This function should run forever.
			gameServer.manageGames();
		} catch (NamingException | JMSException | InterruptedException e) {
			System.err.println("Program aborted");
		} finally {
			if (gameServer != null) {
				try {
					gameServer.close();
				} catch (Exception e) {
				}
			}
		}
	}

	public GameServer(String host) throws NamingException, JMSException {
		this.host = host;
		try {
			dbConn = new DBConnection();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}

		localPlayerQueue = new LinkedList<String>();
		localPotentialGames = new LinkedList<PotentialGame>();
		localActiveGames = new LinkedList<Game>();
		localGameInputs = new LinkedList<String>();
		
		queueReceiver = null;

		// Access JNDI
		createJNDIContext();

		// Lookup JMS resources
		lookupConnectionFactory();
		lookupQueues();
		

		// Create connection->session->sender
		createConnection();
		createSession();
		createActiveGameQueueSender();

		clearQueues(); // fight me
	}

	public void manageGames() throws InterruptedException {

		while (true) {
			System.out.println("Running Game management loop");

			activateGamesThatAreReady();

			// attempt to organise current players into games in to games
			if (localPlayerQueue.size() > 0) {
				System.out.println("Local player queue is > 0");
				for (int p = 0; p < localPlayerQueue.size(); p++) {

					// Message is delivered as csv
					String msg = localPlayerQueue.pop();

					Player newPlayer;
					try {
						String[] msgComponents = msg.split(",");
						newPlayer = new Player(msgComponents[0], msgComponents[1]);
					} catch (Exception e) {
						System.out.println("Failed to read a message as 2 components separated by a comma");
						System.out.println(e.getStackTrace());
						newPlayer = null;
						continue; // Skip this loop iteration, the message is useless
					}

					boolean gameFound = false;
					for (int g = 0; g < localPotentialGames.size(); g++) {
						PotentialGame pg = localPotentialGames.get(g);
						System.out.println("Potential games in queue: " + localPotentialGames.size());
						if (pg.game.isTherASpareSlot()) {
							// Ding Ding! Spare slot available for this player
							System.out.println("Game found for: " + newPlayer.id);
							gameFound = true;
							pg.game.addPlayer(newPlayer);
							//pg.ResetWaitingTime();
							pg.game.printPlayers();
						}
					}

					if (gameFound == false) {
						// There was no available game for this player, make a new one
						PotentialGame pg = new PotentialGame(newPlayer);
						localPotentialGames.add(pg);
						System.out.println("No available games for " + newPlayer.id + ", created a new one.");
					}
				}
			}

			// get all waiting players, add the to the local linked list of players
			try {
				// System.err.println("Trying to receive playerqueue");
				LinkedList<String> msgs = receiveMessages(playerQueue);
				localPlayerQueue = extend(localPlayerQueue, msgs);
				System.out.println("Local player queue size: " + localPlayerQueue.size());
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			processGameInput();
			finishGamesThatAreFinished();
			incrementTimers(12); // TODO: Actually calculate the time because the JMS calls are inconsistent
		}
	}
	
	/*
	 * Extend one linkedlist with another
	 */
	public LinkedList<String> extend(LinkedList<String> base, LinkedList<String> extension) {
		for (int i = 0; i < extension.size(); i++) {
			base.add(extension.get(i));
		}
		return base;
	} 
	
	
	/*
	 * Handle all gameinput that has been received
	 */
	private void processGameInput() {
		System.out.println("ProcessGameInput, " + localActiveGames.size() + " active games");
		LinkedList<String> msgs;
		// pull down queue
		try {
			msgs = receiveMessages(gameInputQueue);
			localGameInputs = extend(localGameInputs, msgs);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		// Pull all the active games, and process the list of messages
		try {
			updateLocalActiveGameList();
			while (msgs.size() > 0) {
				processGameInputMessage(msgs.pop());
			}
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	private void finishGamesThatAreFinished() {
		// if all players have answered, calculate gameresults, add []
		int nGames = localActiveGames.size();
		if(nGames == 0)
			return;
		
		System.out.println("Local Active Games: "+nGames);
		for (int i = 0; i < nGames; i++) {
			Game g = localActiveGames.get(i);
			if (g.haveAllPlayersAnswered()) {
				System.out.println("All player have answered in a game, finishing.");
				// Create GameResults
				processGameResult(new GameResult(g, g.getWinners()));
				
				// Remove this game from the localActiveGame queue, break the loop to avoid weird indexing
				localActiveGames.remove(i);
				return;
			}
		}	
	}
	
	private void processGameResult(GameResult gR) {	
		// to "GameResults" queue
		try {
			submitGameResultToQueue(gR);
		} catch (JMSException e) {
			// TODO Auto-generated catch block			
			e.printStackTrace();
		}
		
		// Update DB
		dbConn.addGameResultToDb(gR);
	}
	
	public void submitGameResultToQueue(GameResult gR) throws JMSException {
		String[] winners = gR.getWinners();
		Game game = gR.getGame();

		createGameResultQueueSender();
		
		String winnersStr = "";
		for (int i = 0; i < winners.length; i ++) {
			if(winners[i] != null)
			{
				winnersStr += winners[i];
				winnersStr += "&";
			}
		}
		// remove the last 'char'
		winnersStr = winnersStr.substring(0, winnersStr.length() - 1);
		
		TextMessage msg = sess.createTextMessage();
		msg.setText(game.getId() + "," + winnersStr);
		System.out.println("Submitting GameResult: "+msg.getText());
		gameResultQueueSender.send(msg);
		// send non-text control message to end
		// playerQueueSender.send(sess.createMessage());
	}
	
	/*
	 *  The assumption here is that localActiveGameQueue is up to date
	 *  message format: p.id + ", " + g.GetId() + ", " + answer
	 */
	private void processGameInputMessage(String msg) {
		System.out.println("processing: "+ msg);
		// parse message 
		String[] components = msg.split(",");
		Game relevantGame = null;
		
		for (int i = 0; i< localActiveGames.size(); i++) {
			if (localActiveGames.get(i).getId().equals(components[1])) {
				relevantGame = localActiveGames.get(i);				
			}
		}
		// Find which slot this player sits in, and set the answer accordingly
		for (int i = 0; i < 4; i++) {
			if (relevantGame.getPlayers()[i].getId().equals(components[0])) {
				relevantGame.setAnswer(i, components[2]);
			}
		}
	}
	
	private void updateLocalActiveGameList() throws JMSException {
		LinkedList<String> msgs = receiveMessages(activeGameQueue);
		for (int i = 0; i < msgs.size(); i++) {
			// Pull all the activeGames from the list, and add them to the local activeGameList
			localActiveGames.add(decodeGameMessage(msgs.get(i)));
		}
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

	private void activateGame(PotentialGame pg, int indexInLocalList) throws JMSException {
		String gameId = dbConn.getValidId("game");
		String players = pg.game.getPlayersString("&");
		String cards = pg.game.getCardsString("&");
		// Ensure the game has a unique Id
		pg.game.initialiseId(gameId);

		dbConn.addGameToDB(pg.game);
		

		TextMessage msg = sess.createTextMessage();

		// csv the values
		msg.setText(gameId + "," + players + "," + cards);
		activeGameQueueSender.send(msg);
		
		/* left commmented, becuase deactivating seems to have no efffect, but the exmaple code includes it? */
		// send non-text control message to end
		// activeGameQueueSender.send(sess.createMessage()); 

		// Now that game is activated, we can remove it from the local potentialGame list
		localPotentialGames.remove(indexInLocalList);
	}

	private void activateGamesThatAreReady() {
		// games that have waited long enough can start
		for (int g = 0; g < localPotentialGames.size(); g++) {
			PotentialGame thisPg = localPotentialGames.get(g);

			// If waiting longer than 10 sec, and > 1 player...
			if (thisPg.waitingTime > 10 && thisPg.game.getNumberOfPlayers() > 1) {
				// Game can start!
				System.out.println(
						"Game {} has been waiting: " + thisPg.waitingTime + " seconds. It will now be started.");
				try {
					activateGame(thisPg, g);
				} catch (JMSException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	private void incrementTimers(int i) throws InterruptedException {
		//TimeUnit.SECONDS.sleep(i);
		// Increment game waiting timers by one second
		for (int g = 0; g < localPotentialGames.size(); g++) {
			localPotentialGames.get(g).incrementWaitingTime(i);
		}
	}

	/*
	 * Create the jndicontext, and allow this program to see the glassfish stuff
	 */
	private void createJNDIContext() throws NamingException {
		System.setProperty("org.omg.CORBA.ORBInitialHost", host);
		System.setProperty("org.omg.CORBA.ORBInitialPort", "3700");
		try {
			jndiContext = new InitialContext();
		} catch (NamingException e) {
			System.err.println("Could not create JNDI API context: " + e);
			throw e;
		}
	}

	/*
	 * Find the connection factory in the current jndi context
	 */
	private void lookupConnectionFactory() throws NamingException {

		try {
			connectionFactory = (ConnectionFactory) jndiContext.lookup("jms/GameConnectionFactory");
		} catch (NamingException e) {
			System.err.println("JNDI API JMS connection factory lookup failed: " + e);
			throw e;
		}
	}

	/*
	 * Establish JMS object reference
	 */
	private void lookupQueues() throws NamingException {
		try {
			playerQueue = (Queue) jndiContext.lookup("jms/PlayerQueue");
			activeGameQueue = (Queue) jndiContext.lookup("jms/ActiveGameQueue");
			gameInputQueue = (Queue) jndiContext.lookup("jms/GameInputQueue");
			gameResultQueue = (Queue) jndiContext.lookup("jms/GameResultQueue");
		} catch (NamingException e) {
			System.err.println("JNDI API JMS queue lookup failed: " + e);
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

	/*
	 * Parametrised, specify the JMS queue to pull from, as well as the local queue
	 * to store results in
	 */
	public LinkedList<String> receiveMessages(Queue relevantQueue) throws JMSException {
		createReceiver(relevantQueue);
		LinkedList<String> msgsReturn = new LinkedList<String>();
		
		while (true) {
			// The library is designed to hang forever on this until a message is received.
			Message m = queueReceiver.receive(1000);// 1000 => timeout of 1 second
			if (m != null && m instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) m;
				System.err.println("Received message: " + textMessage.getText());
				msgsReturn.add(textMessage.getText());

			} else {
				queueReceiver.close();
				break;
			}
		}
		return msgsReturn;
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

	public void killMessages(Queue relevantQueue) throws JMSException {
		System.err.println("Killing queue messages..");
		createReceiver(relevantQueue);
		while (true) {
			Message m = queueReceiver.receive(100); // wait 100ms
			if (m != null && m instanceof TextMessage) {
				continue;// Keep going until they're all consumed
			} else {
				queueReceiver.close();
				break;
			}
		}
	}
	
	private void createActiveGameQueueSender() throws JMSException {
		try {
			activeGameQueueSender = sess.createProducer(activeGameQueue);
		} catch (JMSException e) {
			System.err.println("Failed to create session: " + e);
			throw e;
		}
	}
	

	private void createGameResultQueueSender() throws JMSException {
		try {
			gameResultQueueSender = sess.createProducer(gameResultQueue);
		} catch (JMSException e) {
			System.err.println("Failed to create session: " + e);
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

	private void createReceiver(Queue relevantQueue) throws JMSException {
		try {
			queueReceiver = sess.createConsumer(relevantQueue);
		} catch (JMSException e) {
			System.err.println("Failed to create session: " + e);
			throw e;
		}
	}

	public void clearQueues() {
		try {
			if (playerQueue != null) {
				killMessages(playerQueue);
			}
			if (activeGameQueue != null) {
				killMessages(activeGameQueue);
			}
			if (gameInputQueue != null) {
				killMessages(gameInputQueue);
			}
			if (gameResultQueue != null)
				killMessages(gameResultQueue);
			
		} catch (JMSException e) {
			System.err.println("Failed to kill queue messages");
			e.printStackTrace();
		}
	}

	public void close() {
		clearQueues();
		if (conn != null) {
			try {
				conn.close();
			} catch (JMSException e) {
			}
		}
	}
}
