
/**
*	@file NotifyClient.java
*	@author Chiara Maggi 578517
*/
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NotifyClient extends RemoteObject implements NotifyClientInterface {
    private LinkedList<String> followers;
    private Lock lock;

    public NotifyClient(LinkedList<String> followers) {
        this.followers = followers;
        lock = new ReentrantLock();
    }

    /* Metodi per l'aggiornamento della lista di followers */
    public void notifyNewFollower(String username) throws RemoteException {
        try {
            lock.lock();
            followers.add(username);
        } finally {
            lock.unlock();
        }
    }

    public void notifyNewUnfollower(String username) throws RemoteException {
        try {
            lock.lock();
            followers.remove(username);
        } finally {
            lock.unlock();
        }

    }

}
