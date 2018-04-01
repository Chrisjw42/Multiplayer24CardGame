import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;


public class QueueReceiverExample {

	public static void main(String [] args) {
		String host = "localhost";
		QueueReceiverExample receiver = null;
		try {
			receiver = new QueueReceiverExample(host);
			receiver.receiveMessages();
		} catch (NamingException | JMSException e) {
			System.err.println("Program aborted");
		} finally {
			if(receiver != null) {
				try {
					receiver.close();
				} catch (Exception e) { }
			}
		}
	}
	
	private String host;
	public QueueReceiverExample(String host) throws NamingException, JMSException {
		this.host = host;
		
		// Access JNDI
		createJNDIContext();
		
		// Lookup JMS resources
		lookupConnectionFactory();
		lookupQueue();
		
		// Create connection->session->sender
		createConnection();

	}
	
	private Context jndiContext;
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
	
	private ConnectionFactory connectionFactory;
	private void lookupConnectionFactory() throws NamingException {

		try {
			connectionFactory = (ConnectionFactory)jndiContext.lookup("jms/TestConnectionFactory");
		} catch (NamingException e) {
			System.err.println("JNDI API JMS connection factory lookup failed: " + e);
			throw e;
		}
	}
	
	private Queue queue;
	private void lookupQueue() throws NamingException {

		try {
			queue = (Queue)jndiContext.lookup("jms/TestQueue");
		} catch (NamingException e) {
			System.err.println("JNDI API JMS queue lookup failed: " + e);
			throw e;
		}
	}
	
	private Connection connection;
	private void createConnection() throws JMSException {
		try {
			connection = connectionFactory.createConnection();
			connection.start();
		} catch (JMSException e) {
			System.err.println("Failed to create connection to JMS provider: " + e);
			throw e;
		}
	}
	
	
	
	
	
	
	
	
	
	public void receiveMessages() throws JMSException {
		createSession();
		createReceiver();
		
		while(true) {
			Message m = queueReceiver.receive();
			if(m != null && m instanceof TextMessage) {
				TextMessage textMessage = (TextMessage)m;
				System.out.println("Received message: "+textMessage.getText());
			} else {
				break;
			}
		}
	}
	
	private Session session;
	private void createSession() throws JMSException {
		try {
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		} catch (JMSException e) {
			System.err.println("Failed to create session: " + e);
			throw e;
		}
	}
	
	// Was: QueueReceiver
	private MessageConsumer queueReceiver;
	private void createReceiver() throws JMSException {
		try {
			queueReceiver = session.createConsumer(queue);
		} catch (JMSException e) {
			System.err.println("Failed to create session: " + e);
			throw e;
		}
	}
	
	public void close() {
		if(connection != null) {
			try {
				connection.close();
			} catch (JMSException e) { }
		}
	}
}
