
/**
*	@file ServerRemoteInterface.java
*	@author Chiara Maggi 578517
*/
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;

public interface ServerRemoteInterface extends Remote {

    public boolean register(String username, String password, LinkedList<String> tag_list) throws RemoteException;

    public LinkedList<String> backupFollowers(String username) throws RemoteException;

    public void registerForCallback(NotifyClientInterface ClientInterface, String username) throws RemoteException;

    public void unregisterForCallback(NotifyClientInterface ClientInterface, String username) throws RemoteException;
}
