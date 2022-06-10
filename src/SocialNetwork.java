import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class SocialNetwork extends RemoteObject implements ServerRemoteInterface {

    private ConcurrentHashMap<Long, Post> posts;
    private ConcurrentHashMap<String, User> users;
    // valore univoco per i post che vengono creati
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

    public long createPost(String author, String title, String content) {
        long id = postId.addAndGet(1);
        Post post = new Post(id, author, title, content);
        User user = users.get(author);
        if (posts.putIfAbsent(id, post) == null) {
            // TODO: gestione delle lock

            // modifico il blog dell'autore
            user.addPostToBlog(post);
            // modifico il feed dei followers
            for (String s : user.getFollowers()) {
                users.get(s).addPostToFeed(post);
            }
            return id;
        } else
            return 0;
    }

    public String showPost(Long id) {
        Post post;
        String printedPost = "";
        if ((post = posts.get(id)) != null) {
            printedPost = "< Title: " + post.getTitle() + "\n< Content: " + post.getContent() + "\n< Votes: "
                    + "\n<    Positive: " + post.getPositiveVotes() + "\n<    Negative: " + post.getNegativeVotes()
                    + "\n< Comments: " + post.getNumComments() + "\n" + post.getComments();
            return printedPost;
        }
        return null;
    }

    public boolean addComment(String username, Long id, String comment) {
        Post post;
        if ((post = posts.get(id)) != null) {
            // TODO: aggiungere tutti i controlli mancanti
            post.addComment(username, comment);
            return true;
        }
        return false;
    }

    public String viewBlog(String username) {
        User user = users.get(username);
        HashMap<Long, Post> blog = user.getBlog();
        String blogInString = "";
        for (Long key : blog.keySet()) {
            blogInString = blogInString
                    .concat("< " + key + "       | " + blog.get(key).getAuthor() + "        | "
                            + blog.get(key).getTitle()
                            + "\n");
        }
        return blogInString;
    }

    public String showFeed(String username) {
        User user = users.get(username);
        HashMap<Long, Post> feed = user.getFeed();
        String feedInString = "";
        for (Long key : feed.keySet()) {
            feedInString = feedInString
                    .concat("< " + key + "       | " + feed.get(key).getAuthor() + "        | "
                            + feed.get(key).getTitle()
                            + "\n");
        }
        return feedInString;
    }

    public boolean followUser(String username, String usernameToFollow) {
        User userToFollow = users.get(usernameToFollow);
        User user = users.get(username);
        // controllo se l'utente segue già userFollowed
        if (userToFollow.getFollowers().contains(username))
            return false;
        else {
            // TO DO: gestire lock e callback
            user.addFollowed(usernameToFollow);
            userToFollow.addFollowers(username);

            // aggiunta di tutti i post di userToFollow al feed di user
            for (Post p : userToFollow.getBlog().values()) {
                user.addPostToFeed(p);
            }
        }
        return true;
    }

    public boolean unfollowUser(String username, String usernameToUnfollow) {
        User userToUnfollow = users.get(usernameToUnfollow);
        User user = users.get(username);
        // controllo se l'utente segue userToUnfollow
        if (userToUnfollow.getFollowers().contains(username)) {
            // TO DO: gestire lock e callback
            user.getFollowed().remove(usernameToUnfollow);
            userToUnfollow.getFollowers().remove(username);

            for (Long id : userToUnfollow.getBlog().keySet()) {
                user.removePostFromFeed(id);
            }
        } else {
            return false;
        }
        return true;
    }

    public User getUser(String username) {
        return users.get(username);
    }

}
