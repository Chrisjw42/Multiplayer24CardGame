
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {
	Boolean login(String loginName, String loginPass) throws RemoteException;
	Boolean register(String loginName, String loginPass) throws RemoteException;
	void clearOnlineUsers() throws RemoteException;
}