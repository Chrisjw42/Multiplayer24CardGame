
public class ShutDownThread extends Thread{
	// This thread is called when the server closes (cleanly), and emptys the JMS queues 
	private GameServer gS;
	
	public ShutDownThread(GameServer gsPass) {
		gS = gsPass;
	}
	
	@Override
	public void run() {
		System.err.println("ShutDownThread was called successfully!");
		gS.clearQueues();
	}
}
