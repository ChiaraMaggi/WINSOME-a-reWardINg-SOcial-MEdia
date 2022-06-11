import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NotifyClientInterface extends Remote {
    public void notifyNewFollower(String username) throws RemoteException;

    public void notifyNewUnfollower(String username) throws RemoteException;
}
