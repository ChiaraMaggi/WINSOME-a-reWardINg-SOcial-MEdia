
/**
*	@file User.java
*	@author Chiara Maggi 578517
*/
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

public class User {
    private final String username;
    private final String seed;
    private final String hashedPassword;
    private final ConcurrentHashMap<Long, Post> blog;
    private final ConcurrentHashMap<Long, Post> feed;
    private final LinkedList<String> tags;
    private final LinkedList<String> followers;
    private final LinkedList<String> followed;
    private final LinkedList<Long> votes; // elenco dei post votati dall'utente
    private final Wallet wallet;
    private boolean logged = false; // per tener traccia se un utente è loggato su un client
    private final ReentrantLock followersLock;

    /* Costruttore usato nel caso di nuovi utenti */
    public User(String username, String password, LinkedList<String> tags) throws NoSuchAlgorithmException {
        byte[] array = new byte[32];
        ThreadLocalRandom.current().nextBytes(array);

        this.username = username;
        this.tags = tags;
        this.seed = new String(array, StandardCharsets.UTF_8);
        hashedPassword = Hash.bytesToHex(Hash.sha256(password + seed));
        blog = new ConcurrentHashMap<>();
        feed = new ConcurrentHashMap<>();
        followers = new LinkedList<>();
        followed = new LinkedList<>();
        votes = new LinkedList<>();
        wallet = new Wallet();
        followersLock = new ReentrantLock();

    }

    /* Costruttore usato per il ripristino degli utenti */
    public User(String username, String hashedPassword, String seed, LinkedList<String> tags,
            LinkedList<String> followers, LinkedList<String> followed, LinkedList<Long> votes,
            ConcurrentHashMap<Long, Post> blog, ConcurrentHashMap<Long, Post> feed, Wallet wallet) {
        this.username = username;
        this.tags = tags;
        this.seed = seed;
        this.hashedPassword = hashedPassword;
        this.blog = blog;
        this.feed = feed;
        this.followers = followers;
        this.followed = followed;
        this.votes = votes;
        this.wallet = wallet;
        followersLock = new ReentrantLock();
    }

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

    public void followersLock() {
        followersLock.lock();
    }

    public void followersUnlock() {
        followersLock.unlock();
    }

    /* Metodi getter */
    public String getUsername() {
        return username;
    }

    public String getSeed() {
        return seed;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public LinkedList<String> getTags() {
        return tags;
    }

    public ConcurrentHashMap<Long, Post> getBlog() {
        return blog;
    }

    public ConcurrentHashMap<Long, Post> getFeed() {
        return feed;
    }

    public LinkedList<String> getFollowers() {
        return followers;
    }

    public LinkedList<String> getFollowed() {
        return followed;
    }

    public LinkedList<Long> getListVotes() {
        return votes;
    }

    public Wallet getWallet() {
        return wallet;
    }

    /* Metodi adder */
    public void addPostToBlog(Post p) {
        blog.putIfAbsent(p.getId(), p);
    }

    public void addPostToFeed(Post p) {
        feed.putIfAbsent(p.getId(), p);
    }

    public void addFollowers(String username) {
        followers.add(username);
    }

    public void addFollowed(String username) {
        followed.add(username);
    }

    public void addIdToListVotes(Long id) {
        votes.add(id);
    }

    /* Metodi remove */
    public void removePostFromBlog(Long id) {
        blog.remove(id);
    }

    public void removePostFromFeed(Long id) {
        feed.remove(id);
    }

    /* Metodo di utilità per stampare i tags */
    public String printTags(LinkedList<String> tags) {
        String print = "";
        int i;
        for (i = 0; i < tags.size() - 1; i++) {
            print = print.concat(tags.get(i) + ", ");
        }
        print = print.concat(tags.get(tags.size() - 1));
        return print;
    }
}
