import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class SocialNetwork extends RemoteObject implements ServerRemoteInterface {

    private ConcurrentHashMap<Long, Post> posts;
    private ConcurrentHashMap<String, User> users;
    private final ConcurrentHashMap<String, NotifyClientInterface> callbacksRegistration; // utenti registrati alle
                                                                                          // callback
    // valore univoco per i post che vengono creati
    private volatile AtomicLong postId;

    public SocialNetwork() {
        posts = new ConcurrentHashMap<>();
        users = new ConcurrentHashMap<>();
        callbacksRegistration = new ConcurrentHashMap<>();
        postId = new AtomicLong(0);
    }

    public boolean register(String username, String password, LinkedList<String> tags) throws RemoteException {
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

    // utilizzata quando un certo utente si riconette sul client, per recuperare la
    // lista dei followers gestita lato client
    public LinkedList<String> backupFollowers(String username) throws RemoteException {
        User user;
        LinkedList<String> followers;
        if ((user = users.get(username)) != null) {
            followers = new LinkedList<>(user.getFollowers());
            return followers;
        }
        return new LinkedList<>();

    }

    public synchronized void registerForCallback(NotifyClientInterface ClientInterface, String username)
            throws RemoteException {
        callbacksRegistration.putIfAbsent(username, ClientInterface);
    }

    public synchronized void unregisterForCallback(NotifyClientInterface ClientInterface, String username)
            throws RemoteException {
        callbacksRegistration.remove(username, ClientInterface);
    }

    public synchronized boolean doCallbackFollow(String usernameFollowed, String follower) {
        NotifyClientInterface client = callbacksRegistration.get(usernameFollowed);
        try {
            client.notifyNewFollower(follower);
            return true;
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e) {
            return true;
        }
    }

    public synchronized boolean doCallbackUnfollow(String usernameUnfollowed, String unfollower) {
        NotifyClientInterface client = callbacksRegistration.get(usernameUnfollowed);
        try {
            client.notifyNewUnfollower(unfollower);
            return true;
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e) {
            return true;
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
                    + "\n< Comments: " + post.getNumComments() + "\n" + post.getCommentsInString();
            return printedPost;
        }
        return null;
    }

    public boolean addComment(String username, Long id, String comment) {
        Post post;
        User user = users.get(username);
        if ((post = posts.get(id)) != null) {
            // controllo se il commentante non è l'autore del
            // post e se ha il post nel proprio feed
            if (post.getAuthor() != username && user.getFeed().get(id) != null) {
                post.addComment(username, comment);
                return true;
            }
        }
        return false;
    }

    public String viewBlog(String username) {
        User user = users.get(username);
        HashMap<Long, Post> blog = user.getBlog();
        String blogInString = "";
        for (Long key : blog.keySet()) {
            blogInString = blogInString
                    .concat(key + "       | " + blog.get(key).getAuthor() + "        | "
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
                    .concat(key + "       | " + feed.get(key).getAuthor() + "        | "
                            + feed.get(key).getTitle()
                            + "\n");
        }
        return feedInString;
    }

    public boolean followUser(String follower, String usernameToFollow) {
        User userToFollow = users.get(usernameToFollow);
        User user = users.get(follower);
        // controllo se l'utente segue già userFollowed
        if (userToFollow.getFollowers().contains(follower))
            return false;
        else {
            // TO DO: gestire lock e callback
            user.addFollowed(usernameToFollow);
            userToFollow.addFollowers(follower);

            // aggiunta di tutti i post di userToFollow al feed di user
            for (Post p : userToFollow.getBlog().values()) {
                user.addPostToFeed(p);
            }
            doCallbackFollow(usernameToFollow, follower);
        }
        return true;
    }

    public boolean unfollowUser(String unfollower, String usernameToUnfollow) {
        User userToUnfollow = users.get(usernameToUnfollow);
        User user = users.get(unfollower);
        // controllo se l'utente segue userToUnfollow
        if (userToUnfollow.getFollowers().contains(unfollower)) {
            // TO DO: gestire lock e callback
            user.getFollowed().remove(usernameToUnfollow);
            userToUnfollow.getFollowers().remove(unfollower);

            // rimozione di tutti i post dell'utente che è stato smesso di seguire
            // almeno che il post non sia stato ricondiviso da un altro utente seguito
            for (Long id : userToUnfollow.getBlog().keySet()) {
                for (String u : user.getFollowed()) {
                    User followed = users.get(u);
                    if (!followed.getBlog().containsKey(id))
                        user.removePostFromFeed(id);
                }
            }
            doCallbackUnfollow(usernameToUnfollow, unfollower);
            return true;
        }
        return false;

    }

    public boolean deletePost(Long id, String username) {
        Post post;
        if ((post = posts.get(id)) != null) {
            if ((post.getAuthor()).equals(username)) {
                // rimozione dall'insieme generale dei post
                posts.remove(id);

                User userAuthor = users.get(post.getAuthor());
                // rimozione dal blog dell'autore
                userAuthor.removePostFromBlog(id);
                // TO DO: risolvere problema che se una persona che seguo ricondivide un mio
                // post ed elimino il post per lui tutto apposto ma io continuo a vedere il
                // postricondiviso nel mio feed -> controllare
                userAuthor.removePostFromFeed(id);

                // rimozione dal feed e blog(in caso di rewin) di tutti i follower
                for (String u : userAuthor.getFollowers()) {
                    users.get(u).removePostFromBlog(id);
                    users.get(u).removePostFromFeed(id);
                }

                return true;
            }
        }
        return false;
    }

    public boolean ratePost(Long id, int vote, String username) {
        Post post;
        User user = users.get(username);
        if ((post = posts.get(id)) != null) { // il post deve esistere
            // controllo che il votante non sia l'autore del post, che abbia il post nel
            // feed e che non abbia già votato
            if (post.getAuthor() != username && user.getFeed().containsKey(id) && !user.getListVotes().contains(id)) {
                post.addLike(username, vote);
                user.addIdToListVotes(id);
                return true;
            }
        }
        return false;
    }

    public boolean rewinPost(Long id, String username) {
        Post post;
        User user = users.get(username);
        if ((post = posts.get(id)) != null) {
            // controllo che il post sia nel feed dell'utente che vuole ricondividere
            if (user.getFeed().containsKey(id)) {

                // aggiungo il post al blog dell'utente
                user.addPostToBlog(post);

                // aggiungo il post al feed dei followers dell'utente che fa il rewin
                User follower;
                for (String u : user.getFollowers()) {
                    follower = users.get(u);
                    follower.addPostToFeed(post);
                }

                return true;
            }
        }
        return false;
    }

    public String listUsers(String username) {
        User user = users.get(username);
        String listUsers = "";
        LinkedList<String> tags = user.getTags();
        for (String s : users.keySet()) {
            if (!s.equals(username)) {// non può seguire se stesso
                User u = users.get(s);
                for (String tag : tags) {
                    if (u.getTags().contains(tag)) {
                        // TODO: incolonnare bene i campi
                        listUsers = listUsers.concat(s + "      |   " + u.printTags(u.getTags()) + "\n");
                    }
                }
            }
        }
        return listUsers;
    }

    public String listFollowing(String username) {
        User user = users.get(username);
        String listFollowing = "";
        for (String s : user.getFollowed()) {
            User u = users.get(s);
            listFollowing = listFollowing.concat(s + "      |   " + u.printTags(u.getTags()) + "\n");
        }
        return listFollowing;
    }

    public double toBitcoin(double total) throws IOException {
        URL randomOrg = new URL("https://www.random.org/decimal-fractions/?num=1&dec=10&col=2&format=plain&rnd=new");
        InputStream urlReader = randomOrg.openStream();
        BufferedReader buff = new BufferedReader(new InputStreamReader(urlReader));
        String randomValue;
        randomValue = buff.readLine();
        buff.close();
        urlReader.close();

        return Double.parseDouble(randomValue) * total;
    }

    public User getUser(String username) {
        return users.get(username);
    }

    public Post getPost(Long id) {
        return posts.get(id);
    }

    public ConcurrentHashMap<String, User> getAllUsers() {
        return users;
    }

    public ConcurrentHashMap<Long, Post> getAllPosts() {
        return posts;
    }

    public void setAllUsers(ConcurrentHashMap<String, User> users) {
        this.users = users;
    }

    public void setAllPosts(ConcurrentHashMap<Long, Post> posts) {
        this.posts = posts;
    }
}
