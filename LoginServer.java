
import java.rmi.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;

import java.rmi.server.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

// SERVER 

public class LoginServer extends UnicastRemoteObject
implements RemoteInterface
{
	private static final long serialVersionUID = 1L;

	// Just a tester
	public static void main(String[] args) {
		try {
			LoginServer app = new LoginServer();
			app.clearOnlineUsers();
			//LocateRegistry.createRegistry(2020);
			//System.setProperty("java.rmi.server.hostname", "192.168.1.88");
	
			// Creating a new security manager to allow name binding
			System.setProperty("java.security.policy", "security.policy");
			System.setSecurityManager(new SecurityManager());
			Naming.rebind("LoginServer", app);
			System.out.println("Service Registered!");
			
		} catch(Exception e) {
			System.err.println("Exception thrown: "+e);
		}
	}
	
	public LoginServer() throws RemoteException{}
	
	public void clearOnlineUsers() {
		// Wipe the file on startup, no users online initially.
		writeToFile("ONLINE USERS:", "OnlineUser.txt", false);
		
	}
	
	public boolean writeToFile(String text, String file, Boolean append) {
		System.out.println("writeToFile(): " + text +" "+ file);
		FileWriter fw = null;
		BufferedWriter bw = null;
		PrintWriter pw = null;
		try
		{
			// FW second parameter is for appending
			fw = new FileWriter(file, append);
			bw = new BufferedWriter(fw);	
			pw = new PrintWriter(bw);
			pw.println(text);
			
		} catch(IOException e) {

			System.out.println(text+" IOE failed to save to "+file);
			System.out.println(e.getMessage());
			System.out.println(e.getMessage());
			return false;
		} catch(Exception e) {
			System.out.println(text+" E failed to save to "+file);
			System.out.println(e.getMessage());
			System.out.println(e.getMessage());
			return false;
		} finally { // close the writers if they're open
			try {
				if (pw != null) {
					pw.close();
				}
				if (bw != null) {
					bw.close();
				}
				if (fw != null) {
					fw.close();
				}
			} catch (IOException ee){
				System.out.println(ee.getMessage());
				return false;
			}
		}
		return true;
	}
	
	public List<String> getRegisteredUsers(){
		try {
			List<String> registeredUsers = Files.readAllLines(Paths.get("./UserInfo.txt"));
			return registeredUsers;
		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.out.println("ERROR READING FILE: Returning an empty list of users");
			List<String> empty = new ArrayList<String>();
			return empty;
		}		
	}
	
	public Boolean isUserRegistered(String loginName) {
		List<String> registeredUsers = getRegisteredUsers();
		
		for(int i = 0; i < registeredUsers.size(); i++) {
			// Split this line into username and pass
			List<String> u = Arrays.asList(registeredUsers.get(i).split(","));
			// Compare against the name
			if (loginName.equals(u.get(0))) {
				return true;
			}
		}	
		return false;
	}
	
	public void logout(String loginName) {
		//TODO: everything
	}
		
	public Boolean login(String loginName, String loginPass) throws RemoteException{
		System.out.println("Login service called, user: "+loginName);
		
		List<String> registeredUsers = getRegisteredUsers();
		
		for(int i = 0; i < registeredUsers.size(); i++) {
			List<String> u = Arrays.asList(registeredUsers.get(i).split(","));
			// Compare against the name & pass
			if (loginName.equals(u.get(0))) {
				System.out.println("User found!");
				if(loginPass.equals(u.get(1))) {
					System.out.println("Login Succcessful");
					//JOptionPane.showMessageDialog(null, "Logged in!");
					
					return writeToFile(u.get(0), "OnlineUser.txt", true);
				}
				else {
					JOptionPane.showMessageDialog(null, "Inorrect password!");	
					return false;
				}
			}
		}
		System.out.println(loginName+" Does not exist in the database");
		JOptionPane.showMessageDialog(null, "Error: Login Does not exist!\nPlease register an account.");
		return false;	
	}	
	
	public Boolean register(String loginName, String loginPass) throws RemoteException{
		if(isUserRegistered(loginName)) {
			JOptionPane.showMessageDialog(null, "Error: This username is already in use!");
			return false;
		}
		String uInfo = loginName+","+loginPass;
		// if register success
		
		if(writeToFile(uInfo, "UserInfo.txt", true)) {
			JOptionPane.showMessageDialog(null, "Congratulations on registering! Please log in");
			return true;
		}
		else {
			JOptionPane.showMessageDialog(null, "Error: You were unable to register. Please try a different username.");
			return false;
		}
		
	}
		
}

