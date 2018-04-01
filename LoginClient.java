import java.awt.Dimension;
import java.awt.FlowLayout;
import java.rmi.registry.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.derby.tools.sysinfo;

// CLIENT 

//public class MessageBox implements Runnable, DocumentListener {
public class LoginClient implements Runnable, ActionListener {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new LoginClient(args[0]));
	}
	//private JLabel wordCountLabel;
	//private JTextArea msgBox;
	private RemoteInterface loginServer;
	
	// UI ELEMENTS
	JFrame loginFrame;
	private JLabel lblUser;
	private JLabel lblPwd;
	private JPanel pnlLogin;
	private JButton btnLogin;
	private JTextField txtUsername;
	private JPasswordField txtUserpass;
	private JButton btnRegister;
	
	private RegisterWindow regWindow;
	private MainWindow mainWindow;
	
	private DBConnection dbConn;
	
	public Boolean Validate(String s) {
		//System.out.println(s+s.getClass());
		if (s.length() > 0) {	
			System.out.println("Passed val");
			return true;
		}
		else 
		{
			System.out.println("Failed val");
			return false;
		}
	}
	
	public LoginClient(String host) {
		try {
			dbConn = new DBConnection();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(-1);
		}
		//TODO: Build gameobtainer here instead of on game request (slows down because of glassfish connection)
		
	}
	
	public void loginWindow() {
		FlowLayout layout = new FlowLayout();
		
		loginFrame = new JFrame("Log In! ");
		loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		pnlLogin = new JPanel();
		pnlLogin.setLayout(layout);
		
		btnLogin = new JButton("Login");
		txtUsername = new JTextField(20);
		txtUserpass = new JPasswordField(20);
		btnRegister = new JButton("Register");
		lblUser = new JLabel("Username:");
		lblPwd = new JLabel("Password:");
		
		pnlLogin.add(lblUser);
		pnlLogin.add(txtUsername);
		pnlLogin.add(lblPwd);
		pnlLogin.add(txtUserpass);
		pnlLogin.add(btnLogin);
		pnlLogin.add(btnRegister);
		
		btnLogin.addActionListener(this);
		btnRegister.addActionListener(this);
		
		loginFrame.add(pnlLogin);
		
		pnlLogin.setPreferredSize(new Dimension(320, 120));
		loginFrame.setPreferredSize(new Dimension(320, 120));
		loginFrame.setResizable(false);
				
		loginFrame.pack();
		loginFrame.setLocationRelativeTo(null);
		loginFrame.setVisible(true);
	}
	
	public void run() {
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				// Upon Exit, do this
				try {
					loginServer.clearOnlineUsers();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}));
		
		loginWindow();
		
	}
	
	public Boolean attemptRegister(String uName, String uPwd) {
		loginFrame.setEnabled(false);	
		if(dbConn.doesPlayerExist(uName)) {
			JOptionPane.showMessageDialog(null, "This username is taken. Try again!");
			return false;
		}
		String id = dbConn.getValidPlayerId();
		
		// Player is added to the database, registration complete!
		dbConn.addPlayerToTable(id, uName, uPwd);
		
		loginFrame.setEnabled(true);
		return true;
	}
	
	public void logoutAndReset() {
		mainWindow.main.setVisible(false);
		mainWindow.main.dispose();
		this.run();
			}
	
	private boolean attemptLogin(String uName, String uPass) {
		if (!dbConn.doesPlayerExist(uName)) {
			JOptionPane.showMessageDialog(null, "This username cannot be found, try again or register.");
			return false;
		}
		// player exists
		if (dbConn.checkPlayerPwd(uName, uPass)) {
			return true;
		}
		else {
			JOptionPane.showMessageDialog(null, "This username and password do not match!");
			return false;
		}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		String uName = txtUsername.getText();
		String uPwd = new String(txtUserpass.getPassword());
			System.out.println(uPwd);
		// Logging in
		if (arg0.getActionCommand() == "Login") {
			// If validation fails
			if (!Validate(uName)) {
				JOptionPane.showMessageDialog(null, "Error: Login cannot be blank!");
				return;
			}
			System.out.println(uName+"val passed.");
			System.out.println("=== Login request");
			if (attemptLogin(uName, uPwd)) {
				
				// TODO: Implement playerID
				Player ply = new Player(uName, "abc123");
				
				// The main window is associated with one single player
				mainWindow = new MainWindow(this, ply);
				loginFrame.setVisible(false);
				loginFrame.dispose();
				mainWindow.run();
			}
		}	
		// Registering
		else if(arg0.getActionCommand() == "Register") {
			System.out.println("=== Register request");
			
			loginFrame.setEnabled(false);
			regWindow = new RegisterWindow(this);
			regWindow.run();
			
		}

	}
	
}


