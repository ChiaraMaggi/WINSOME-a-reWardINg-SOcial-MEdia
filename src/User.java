import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

public class User {
    private final String username;
    private final String seed;
    private final String hashedPassword;

    private final HashMap<Long, Post> blog;
    private final HashMap<Long, Post> feed;

    private final LinkedList<String> tags;
    private final LinkedList<String> followers;
    private final LinkedList<String> followed;
    // elenco dei post votati dall'utente
    private final LinkedList<Long> votes;

    private final Wallet wallet;

    private boolean logged = false;

    // costruttore nel caso di nuovi utenti
    public User(String username, String password, LinkedList<String> tags) throws NoSuchAlgorithmException {
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

        votes = new LinkedList<>();

        wallet = new Wallet();

    }

    public User(String username, String hashedPassword, String seed, LinkedList<String> tags,
            LinkedList<String> followers, LinkedList<String> followed, LinkedList<Long> votes,
            HashMap<Long, Post> blog, HashMap<Long, Post> feed, Wallet wallet) {
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

    public void addPostToBlog(Post p) {
        blog.putIfAbsent(p.getId(), p);
    }

    public void addPostToFeed(Post p) {
        feed.putIfAbsent(p.getId(), p);
    }

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

    public HashMap<Long, Post> getBlog() {
        return blog;
    }

    public HashMap<Long, Post> getFeed() {
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

    public void addFollowers(String username) {
        followers.add(username);
    }

    public void addFollowed(String username) {
        followed.add(username);
    }

    public void addIdToListVotes(Long id) {
        votes.add(id);
    }

    public void removePostFromBlog(Long id) {
        blog.remove(id);
    }

    public void removePostFromFeed(Long id) {
        feed.remove(id);
    }

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
