import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class User {
    private final String username;
    private final String tags;
    private final String seed;
    private final String hashedPassword;

    private final HashMap<Long, Post> blog;
    private final HashMap<Long, Post> feed;

    private final LinkedList<String> followers;
    private final LinkedList<String> followed;

    private boolean logged = false;

    // costruttore nel caso di nuovi utenti
    public User(String username, String password, String tags) throws NoSuchAlgorithmException {
        byte[] array = new byte[32];
        ThreadLocalRandom.current().nextBytes(array);

        this.username = username;
        this.tags = tags;
        this.seed = new String(array, StandardCharsets.UTF_8);
        hashedPassword = Hash.bytesToHex(Hash.sha256(password + seed));

        blog = new HashMap<>();
        feed = new HashMap<>();

        followers = new LinkedList<>();
        followed = new LinkedList<>();
    }

    // costruttore in caso di ripristino utenti
    /*
     * public User() {
     * 
     * }
     */

    public boolean isLogged() {
        return logged;
    }

    public boolean verifyPassword(String username, String password) throws NoSuchAlgorithmException {
        if (hashedPassword.compareTo(Hash.bytesToHex(Hash.sha256(password + seed))) == 0
                && (username.compareTo(this.username) == 0))
            logged = true;
        return logged;
    }

    public void logout() {
        logged = false;
        return;
    }

    public void addPostToBlog(Post p) {
        blog.putIfAbsent(p.getId(), p);
    }

    public void addPostToFeed(Post p) {
        feed.putIfAbsent(p.getId(), p);
    }

    public HashMap<Long, Post> getBlog() {
        return blog;
    }

    public HashMap<Long, Post> getFeed() {
        return feed;
    }

    public void addFollowers(String username) {
        followers.add(username);
    }

    public void addFollowed(String username) {
        followed.add(username);
    }

    public LinkedList<String> getFollowers() {
        return followers;
    }

    public LinkedList<String> getFollowed() {
        return followed;
    }

    public void removePostFromFeed(Long id) {
        feed.remove(id);
    }
}
