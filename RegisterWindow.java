
import java.awt.BorderLayout;
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class RegisterWindow implements Runnable, ActionListener {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new LoginClient(args[0]));
	}
	
	private LoginClient parentClient;
	
	// UI ELEMENTS
	private JFrame registerFrame;
	private JLabel lblUser;
	private JLabel lblPwd;
	private JLabel lblPwdConf;
	private JPanel pnlRegister;
	private JTextField txtUsername;
	private JPasswordField txtUserpass;
	private JPasswordField txtUserpassConf;
	private JButton btnRegister;
	private JButton btnCancel;
	
	
	// Pass the client to this window's constructor
	public RegisterWindow(LoginClient client) {
		parentClient = client;
	}
	
	public void run() {
		registerFrame = new JFrame("Log In! ");
		registerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	
		pnlRegister = new JPanel();
		
		txtUsername = new JTextField(20);
		txtUserpass = new JPasswordField(20);
		txtUserpassConf = new JPasswordField(20);
		btnRegister = new JButton("Register");
		btnCancel = new JButton("Cancel");
		lblUser = new JLabel("Username:");
		lblPwd = new JLabel("Password:");
		lblPwdConf = new JLabel("Confirm Password:");
		
		pnlRegister.add(lblUser);
		pnlRegister.add(txtUsername);
		pnlRegister.add(lblPwd);
		pnlRegister.add(txtUserpass);
		pnlRegister.add(lblPwdConf);
		pnlRegister.add(txtUserpassConf);
		pnlRegister.add(btnRegister);
		pnlRegister.add(btnCancel);

		btnRegister.addActionListener(this);
		btnCancel.addActionListener(this);
		
		registerFrame.add(pnlRegister, BorderLayout.CENTER);
		
		pnlRegister.setPreferredSize(new Dimension(300, 230));
		registerFrame.setPreferredSize(new Dimension(300, 230));
		registerFrame.setResizable(false);
		
		registerFrame.pack();
		registerFrame.setLocationRelativeTo(null);
		registerFrame.setVisible(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		// If canceling, don't worry about the rest
		if(arg0.getActionCommand() == "Cancel") {	
			parentClient.loginFrame.setEnabled(true);
			registerFrame.setVisible(false);
			registerFrame.dispose();
			return;
		}
		
		String uName = txtUsername.getText();
		String uPwd = new String(txtUserpass.getPassword());
		String uPwd2 = new String(txtUserpassConf.getPassword());

		// If validation fails
		if (!parentClient.Validate(uName)) {
			JOptionPane.showMessageDialog(null, "Error: Login cannot be blank!");
			return;
		}
		else if (!uPwd.equals(uPwd2)) {
			JOptionPane.showMessageDialog(null, "Error: Pawsswords must match!");
			return;
		}
		
		if(arg0.getActionCommand() == "Register") {	
			if (parentClient.attemptRegister(uName, uPwd)) {
				parentClient.loginFrame.setEnabled(true);
				registerFrame.setVisible(false);
				registerFrame.dispose();				
			}
		}
	}
}
