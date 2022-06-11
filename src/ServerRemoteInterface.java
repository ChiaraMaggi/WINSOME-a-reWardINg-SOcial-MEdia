import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;

public interface ServerRemoteInterface extends Remote {

    public boolean register(String username, String password, LinkedList<String> tag_list) throws RemoteException;

}
