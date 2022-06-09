import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class SocialNetwork extends RemoteObject implements ServerRemoteInterface {

    private ConcurrentHashMap<Long, Post> posts;
    private ConcurrentHashMap<String, User> users;
    private volatile AtomicLong postId;

    public SocialNetwork() {
        posts = new ConcurrentHashMap<>();
        users = new ConcurrentHashMap<>();
        postId = new AtomicLong(0);
    }

    public boolean register(String username, String password, String tags) throws RemoteException {
        if (password.length() > 16 || password.length() < 8) {
            throw new IllegalArgumentException();
        }

        try {
            User newUser = new User(username, password, tags);
            if (users.putIfAbsent(username, newUser) == null)
                return true;
            else
                return false;
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }

    public boolean login(String username, String password) {
        try {
            return (users.containsKey(username) && users.get(username).verifyPassword(username, password));
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }

    public void logout(String username) {
        User user = users.get(username);
        user.logout();
    }

    public boolean createPost(String author, String title, String content) {
        long id = postId.addAndGet(1);
        Post post = new Post(postId, author, title, content);

        return true;
    }

    // metodi getter
    public User getUser(String username) {
        return users.get(username);
    }

    public Post getPostById(long id) {
        return posts.get(id);
    }

}
