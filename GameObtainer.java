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

public class GameObtainer {

	private String host;
	private Context jndiContext;
	private ConnectionFactory connectionFactory;
	private Queue playerQueue;
	private Queue gameQueue;
	private Queue activeGameQueue;
	private Connection conn;
	private Session sess;
	private MessageProducer playerQueueSender;
	private MessageConsumer queueReceiver;
	private LinkedList<String> localActiveGames;

	/*
		public static void main(String [] args) {
			String host = "localhost";
			GameObtainer sender = null;
			try {
				sender = new GameObtainer(host);
				//sender.sendMessages();
			} catch (Exception e) {
				System.err.println("Program aborted");
			} finally {
				if(sender != null) {
					try {
						sender.close();
					} catch (Exception e) { }
				}
			}
		}*/
		
		public GameObtainer(String host){ //TODO: consider passing Player object reference
			this.host = host;
			localActiveGames = new LinkedList<String>();
		}
		
		public boolean Initialise() {
			// Access JNDI
			try {
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
			createSender();	
			TextMessage msg = sess.createTextMessage();
			msg.setText(player.name+","+player.id);
			playerQueueSender.send(msg);
			// send non-text control message to end
			playerQueueSender.send(sess.createMessage());
		}
		
		// Is this even necessary anymore?
		/*
		public void getGameQueue() throws JMSException {
			createSession();
			
			// NOT using createConsumer, which would actually consume the messages			
			// Read queue without consuming messages
			
            QueueBrowser queueBrowser = sess.createBrowser(gameQueue);
			Enumeration msgs = queueBrowser.getEnumeration();
			
			// Get every game in the list
			while (msgs.hasMoreElements()) {
				
				String thisGame = (String) msgs.nextElement();				
				// TODO: check if thisGame's players includes this player
			}
		}*/
		
		public Game DecodeGameMessage(String gameMessage) {
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
			g.SetId(gameId);
			g.SetCards(cards);
			
			// j = 1, because the first player was already passed
			for (int j = 1; j < playerRaw.length; j++) {
				g.SetPlayer(j, players[j]);
			}			
			
			return g;
		}
		
		public Game FindActiveGameWithPlayer(Player player) throws JMSException {		
			// Read queue without consuming messages
            QueueBrowser queueBrowser = sess.createBrowser(activeGameQueue);
			Enumeration msgs = queueBrowser.getEnumeration();
			
			// Get every game in the list
			while (msgs.hasMoreElements()) {
				String thisGame;
				try {
					thisGame = ((TextMessageImpl)msgs.nextElement()).getText();
				}
				catch(ClassCastException e) {
					System.err.println("Tried to parse a message that was not text-based.");
					thisGame = null;
					continue; // Skip this iteration
				}
				
				System.out.println(thisGame);
				
				Game g = DecodeGameMessage(thisGame);
				
				// For this game, we check if any of the players match the player we are searching for
				for (int i = 0; i < 4; i++) {
					System.out.println(g.GetPlayers()[i].id);
					if (g.GetPlayers()[i].id.equals(player.id)) {
						// ding ding! 
						System.out.println("Player: "+player.id+" has found their game, and "
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
		public Game GetGame(Player player) throws JMSException, InterruptedException {
			// Add player to the list of seeking players
			enqueuePlayer(player);
			Game game = null;
			
			// Every 1 second, try and read available game list
			while(game == null) {
				TimeUnit.SECONDS.sleep(1);
				game = FindActiveGameWithPlayer(player);
			}
			return game;
		}
		
		/*
		public void sendMessages() throws JMSException {
			createSession();
			createSender();			
			int count = 1 + new Random().nextInt(10);
			TextMessage message = sess.createTextMessage(); 
			for(int i=1; i<=count; ++i) {
				message.setText("This is message "+i);
				queueSender.send(message);
				System.out.println("Sending message "+i);
			}
			queueSender.send(sess.createMessage());
		}*/
		
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
				connectionFactory = (ConnectionFactory)jndiContext.lookup("jms/GameConnectionFactory");
			} catch (NamingException e) {
				System.err.println("JNDI API JMS connection factory lookup failed: " + e);
				throw e;
			}
		}
		
		private void lookupQueues() throws NamingException {

			try {
				playerQueue = (Queue)jndiContext.lookup("jms/PlayerQueue");
				gameQueue = (Queue)jndiContext.lookup("jms/GameQueue");
				activeGameQueue = (Queue)jndiContext.lookup("jms/ActiveGameQueue");
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
		private void createSender() throws JMSException {
			try {
				playerQueueSender = sess.createProducer(playerQueue);
			} catch (JMSException e) {
				System.err.println("Failed to create session: " + e);
				throw e;
			}
		}
		
		public void close() {
			if(conn != null) {
				try {
					conn.close();
				} catch (JMSException e) { }
			}
		}
		
	}
