import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class GameServer {

	private String host;
	private Context jndiContext;
	private ConnectionFactory connectionFactory;
	private Queue playerQueue;
	private Queue activeGameQueue;
	private LinkedList<String> localPlayerQueue;
	private LinkedList<PotentialGame> localPotentialGames;
	private LinkedList<Game> localActiveGames;
	private Connection conn;
	private Session sess;
	private MessageProducer activeGameQueueSender;


	public static void main(String[] args) {
		String host = "localhost";
		GameServer gameServer = null;
		
		try {
			gameServer = new GameServer(host);

			// Call this thread object when the program shuts down (cleanly)
			ShutDownThread sdT = new ShutDownThread(gameServer);
			Runtime.getRuntime().addShutdownHook(sdT);
			
			// Uncomment this to clear the queues
			//System.exit(0);
			
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
		localPlayerQueue = new LinkedList<String>();
		localPotentialGames = new LinkedList<PotentialGame>();
		localActiveGames = new LinkedList<Game>();

		// Access JNDI
		createJNDIContext();

		// Lookup JMS resources
		lookupConnectionFactory();
		lookupQueues();

		// Create connection->session->sender
		createConnection();
		createSession();
		createSender();	
	}
	
	private void createSender() throws JMSException {
		try {
			activeGameQueueSender = sess.createProducer(activeGameQueue);
		} catch (JMSException e) {
			System.err.println("Failed to create session: " + e);
			throw e;
		}
	}

	private void ActivateGame(PotentialGame pg, int indexInLocalList) throws JMSException {
		// Ensure the game has a unique Id
		pg.game.InitialiseId();
		
		TextMessage msg = sess.createTextMessage();
		String gameId = pg.game.GetId();
		String players = pg.game.GetPlayersString("&");
		String cards = pg.game.GetCardsString("&");
		
		// csv the values
		msg.setText(gameId+","+players+","+cards);
		activeGameQueueSender.send(msg);
		// send non-text control message to end
		//activeGameQueueSender.send(sess.createMessage());
		
		// Now that the game has been activated, we can remove it from the local potentialGame list
		localPotentialGames.remove(indexInLocalList);
	}
	
	private void ActivateGamesThatAreReady() {
		// games that have waited long enough can start
		for (int g = 0; g < localPotentialGames.size(); g++) {
			PotentialGame thisPg = localPotentialGames.get(g);
			
			
			// If waiting longer than 10 sec, and > 1 player...
			if (thisPg.waitingTime > 10 && thisPg.game.GetNumberOfPlayers() > 1) {
				// Game can start!
				System.out.println("Game {} has been waiting: " + thisPg.waitingTime
						+ " seconds. It will now be started.");
				try {
					ActivateGame(thisPg, g);
				} catch (JMSException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void manageGames() throws InterruptedException {

		while (true) {
			System.out.println("Running Game management loop");

			ActivateGamesThatAreReady();

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
						if (pg.game.SpareSlot()) {
							// Ding Ding! Spare slot available for this player
							System.out.println("Game found for: " + newPlayer.id);
							gameFound = true;
							pg.game.AddPlayer(newPlayer);
							pg.ResetWaitingTime();
							pg.game.PrintPlayers();
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
				//System.err.println("Trying to receive playerqueue");
				receiveMessages(playerQueue, localPlayerQueue);
				System.out.println("Local player queue size: "+localPlayerQueue.size());
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
			IncrementTimers(1);
		}
	}
	
	private void IncrementTimers(int i) throws InterruptedException {
		TimeUnit.SECONDS.sleep(i);
		// Increment game waiting timers by one second
		for (int g = 0; g < localPotentialGames.size(); g++) {
			localPotentialGames.get(g).IncrementWaitingTime(i);
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
	public void receiveMessages(Queue relevantQueue, LinkedList<String> localQueue) throws JMSException {
		createReceiver(relevantQueue);
		while (true) {
			// The library is designed to hang forever on this until a message is received.
			Message m = queueReceiver.receive(1000);// 1000 => timeout of 1 second
			if (m != null && m instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) m;
				System.err.println("Received message: " + textMessage.getText());
				localQueue.add(textMessage.getText());
				
			} else {
				queueReceiver.close();
				break;
			}
		}
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

	private void createSession() throws JMSException {
		try {
			sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
		} catch (JMSException e) {
			System.err.println("Failed to create session: " + e);
			throw e;
		}
	}

	private MessageConsumer queueReceiver;

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
		} catch (JMSException e) {
			System.err.println("Failed to kill queue messages on close");
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
