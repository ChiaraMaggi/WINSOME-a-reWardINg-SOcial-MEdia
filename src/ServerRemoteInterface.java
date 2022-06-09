import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerRemoteInterface extends Remote {

    public boolean register(String username, String password, String tag_list) throws RemoteException;

}
