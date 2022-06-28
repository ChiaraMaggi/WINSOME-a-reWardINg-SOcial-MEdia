
/**
*	@file SocialNetwork.java
*	@author Chiara Maggi 578517
*/
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.security.NoSuchAlgorithmException;
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

    /*
     * Metodo utilizzata quando un certo utente si riconette sul client, per
     * recuperare la lista dei followers che viene gestita lato client.
     * Implementazione del metodo dell'interfaccia ServerRemoteInterface.
     */
    public LinkedList<String> backupFollowers(String username) throws RemoteException {
        User user;
        LinkedList<String> followers;
        if ((user = users.get(username)) != null) {
            try {
                user.followersLock();
                followers = new LinkedList<>(user.getFollowers());
            } finally {
                user.followersUnlock();
            }
            return followers;
        }
        return new LinkedList<>();
    }

    /*
     * Metodo per registrare l'utente al servizio di callback per aggiornamento
     * dei followers. Implementazione del metodo dell'interfaccia
     * ServerRemoteInterface.
     */
    public synchronized void registerForCallback(NotifyClientInterface ClientInterface, String username)
            throws RemoteException {
        callbacksRegistration.putIfAbsent(username, ClientInterface);
    }

    /*
     * Metodo per deregistrare l'utente dal servizio di callback per aggiornamento
     * dei followers. Implementazione del metodo dell'interfaccia
     * ServerRemoteInterface.
     */
    public synchronized void unregisterForCallback(NotifyClientInterface ClientInterface, String username)
            throws RemoteException {
        callbacksRegistration.remove(username, ClientInterface);
    }

    /*
     * Metodo che si occupa di notificare al Client l'arrivo di un nuovo follower
     */
    public synchronized boolean doCallbackFollow(String usernameFollowed, String follower) {
        // ricavo l'interfaccia dell'utente che deve ricevere la notifica per il nuovo
        // follower
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

    /*
     * Metodo che si occupa di notificare al Client l'arrivo di un unfollower
     */
    public synchronized boolean doCallbackUnfollow(String usernameUnfollowed, String unfollower) {
        // ricavo l'interfaccia dell'utente che deve ricevere la notifica per il nuovo
        // unfollower
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

    /*
     * Implementazione del metodo dell'interfaccia ServerRemoteInterface che
     * permette di far registrare gli utenti al social Network.
     */
    public boolean register(String username, String password, LinkedList<String> tags) throws RemoteException {
        // controllo lunghezza della password
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

    /* Metodo per effettuare il login */
    public boolean login(String username, String password) {
        try {
            return (users.containsKey(username) && users.get(username).verifyPassword(username, password));
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }

    /* Metodo per effettuare il logout */
    public void logout(String username) {
        User user = users.get(username);
        user.logout();
    }

    /* Metodo per visualizzare la lista di utente con almeno un tag in comune */
    public String listUsers(String username) {
        User user = users.get(username);
        String listUsers = "";
        boolean sameTags = false; // per evitare duplicati quando più di un tag in comune
        LinkedList<String> tags = user.getTags();
        for (String s : users.keySet()) {
            if (!s.equals(username)) {// non può seguire se stesso
                User u = users.get(s);
                for (String tag : tags) {
                    if (u.getTags().contains(tag)) {
                        sameTags = true;
                    }
                }
                if (sameTags) {
                    listUsers = listUsers.concat(String.format("%-15s| ", s) + u.printTags(u.getTags()) + "\n");
                }
            }
        }
        return listUsers;
    }

    /* Metodo per visualizzare la lista di utente seguiti */
    public String listFollowing(String username) {
        User user = users.get(username);
        String listFollowing = "";
        for (String s : user.getFollowed()) {
            User u = users.get(s);
            listFollowing = listFollowing.concat(String.format("%-15s| ", s) + u.printTags(u.getTags()) + "\n");
        }
        return listFollowing;
    }

    /* Metodo per inziare a seguire un utente */
    public boolean followUser(String follower, String usernameToFollow) {
        User userToFollow = users.get(usernameToFollow);
        User user = users.get(follower);
        boolean b = true;
        try {
            userToFollow.followersLock();
            // controllo se l'utente segue già userFollowed
            if (userToFollow.getFollowers().contains(follower))
                b = false;
            else {
                user.addFollowed(usernameToFollow);
                userToFollow.addFollowers(follower);

                // aggiunta di tutti i post di userToFollow al feed di user
                for (Post p : userToFollow.getBlog().values()) {
                    user.addPostToFeed(p);
                }

                // notifica all'utente interessato
                doCallbackFollow(usernameToFollow, follower);
            }
        } finally {
            userToFollow.followersUnlock();
        }
        return b;
    }

    /* Metodo per smettere di seguire un utente */
    public boolean unfollowUser(String unfollower, String usernameToUnfollow) {
        User userToUnfollow = users.get(usernameToUnfollow);
        User user = users.get(unfollower);
        boolean b = false;
        try {
            userToUnfollow.followersLock();
            // controllo se l'utente segue userToUnfollow
            if (userToUnfollow.getFollowers().contains(unfollower)) {
                user.getFollowed().remove(usernameToUnfollow);
                userToUnfollow.getFollowers().remove(unfollower);
                // rimozione di tutti i post dell'utente che è stato smesso di seguire
                // almeno che il post non sia stato ricondiviso da un altro utente seguito
                for (Long id : userToUnfollow.getBlog().keySet()) {
                    boolean keep = false;
                    for (String u : user.getFollowed()) {
                        User followed = users.get(u);
                        if (followed.getBlog().containsKey(id))
                            keep = true;
                    }
                    if (!keep)
                        user.removePostFromFeed(id);
                }
                // notifica all'utente interessato
                doCallbackUnfollow(usernameToUnfollow, unfollower);
                b = true;
            }
        } finally {
            userToUnfollow.followersUnlock();
        }
        return b;

    }

    /* Metodo per visualizzare il blog di un utente */
    public String viewBlog(String username) {
        User user = users.get(username);
        ConcurrentHashMap<Long, Post> blog = user.getBlog();
        String blogInString = "";
        for (Long key : blog.keySet()) {
            blogInString = blogInString
                    .concat(String.format("%-10d| ", key) + String.format("%-15s| ", blog.get(key).getAuthor())
                            + blog.get(key).getTitle() + "\n");
        }
        return blogInString;
    }

    /* Metodo per creare un post */
    public long createPost(String author, String title, String content) {
        long id = postId.addAndGet(1);
        Post post = new Post(id, author, title, content);
        User user = users.get(author);
        if (posts.putIfAbsent(id, post) == null) {
            // modifico il blog dell'autore
            user.addPostToBlog(post);
            // modifico il feed dei followers
            try {
                user.followersLock();
                for (String s : user.getFollowers()) {
                    users.get(s).addPostToFeed(post);
                }
            } finally {
                user.followersUnlock();
            }
            return id;
        } else
            return 0;
    }

    /* Metodo per visualizzare il feed di un utente */
    public String showFeed(String username) {
        User user = users.get(username);
        ConcurrentHashMap<Long, Post> feed = user.getFeed();
        String feedInString = "";
        for (Long key : feed.keySet()) {
            feedInString = feedInString
                    .concat(String.format("%-10d| ", key) + String.format("%-15s| ", feed.get(key).getAuthor())
                            + feed.get(key).getTitle() + "\n");
        }
        return feedInString;
    }

    /* Metodo per visualizzare un post */
    public String showPost(Long id) {
        Post post;
        String printedPost = "";
        if ((post = posts.get(id)) != null) {
            try {
                post.votesLock();
                post.commentsLock();
                printedPost = "< Title: " + post.getTitle() + "\n< Content: " + post.getContent() + "\n< Votes: "
                        + "\n<    Positive: " + post.getPositiveVotes() + "\n<    Negative: " + post.getNegativeVotes()
                        + "\n< Comments: " + post.getNumComments() + "\n" + post.getCommentsInString();
            } finally {
                post.votesUnlock();
                post.commentsUnlock();
            }
            return printedPost;
        }
        return null;
    }

    /* Metodo per rimuovere un post */
    public boolean deletePost(Long id, String username) {
        Post post;
        if ((post = posts.get(id)) != null) {
            if ((post.getAuthor()).equals(username)) {
                // rimozione dall'insieme generale dei post
                posts.remove(id);

                User userAuthor = users.get(post.getAuthor());
                // rimozione dal blog dell'autore
                userAuthor.removePostFromBlog(id);
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

    /* Metodo per fare il rewin di un post */
    public boolean rewinPost(Long id, String username) {
        Post post;
        User user = users.get(username);
        if ((post = posts.get(id)) != null) {
            // controllo che il post sia nel feed dell'utente che vuole ricondividere e che
            // non sia l'autore
            if (user.getFeed().containsKey(id) && !post.getAuthor().equals(username)) {

                // aggiungo il post al blog dell'utente
                user.addPostToBlog(post);

                // aggiungo il post al feed dei followers dell'utente che fa il rewin
                User follower;
                try {
                    user.followersLock();
                    for (String u : user.getFollowers()) {
                        follower = users.get(u);
                        follower.addPostToFeed(post);
                    }
                } finally {
                    user.followersUnlock();
                }
                return true;
            }
        }
        return false;
    }

    /* Metodo per votare un post */
    public boolean ratePost(Long id, int vote, String username) {
        Post post;
        User user = users.get(username);
        if ((post = posts.get(id)) != null) { // il post deve esistere
            // controllo che il votante non sia l'autore del post, che abbia il post nel
            // feed e che non abbia già votato
            if (post.getAuthor() != username && user.getFeed().containsKey(id) && !user.getListVotes().contains(id)) {
                try {
                    post.votesLock();
                    post.addVote(username, vote);
                } finally {
                    post.votesUnlock();
                }

                // per tenere traccia dei post già votati
                user.addIdToListVotes(id);
                return true;
            }
        }
        return false;
    }

    /* Metodo utilizzato per aggiungere un commento ad un post */
    public boolean addComment(String username, Long id, String comment) {
        Post post;
        User user = users.get(username);
        if ((post = posts.get(id)) != null) {
            // controllo se il commentante non è l'autore del
            // post e se ha il post nel proprio feed
            if (post.getAuthor() != username && user.getFeed().get(id) != null) {
                try {
                    post.commentsLock();
                    post.addComment(username, comment);
                } finally {
                    post.commentsUnlock();
                }
                return true;
            }
        }
        return false;
    }

    /* Metodo che si occupa della conversione da Wincoin a Bitcoin */
    public double toBitcoin(double total) throws IOException {
        URL randomOrg = new URL("https://www.random.org/decimal-fractions/?num=1&dec=10&col=2&format=plain&rnd=new");
        InputStream urlReader = randomOrg.openStream();
        BufferedReader buff = new BufferedReader(new InputStreamReader(urlReader));
        String randomValue;
        // ricavo il valore random
        randomValue = buff.readLine();
        buff.close();
        urlReader.close();

        return Double.parseDouble(randomValue) * total;
    }

    /* Metodi getter */
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

    /* Metodi setter */
    public void setAllUsers(ConcurrentHashMap<String, User> users) {
        this.users = users;
    }

    public void setAllPosts(ConcurrentHashMap<Long, Post> posts) {
        this.posts = posts;
    }

    public void setPostId(Long id) {
        postId.set(id);
    }
}
