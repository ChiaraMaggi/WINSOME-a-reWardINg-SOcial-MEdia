
/**
*	@file ServerMain.java
*	@author Chiara Maggi 578517
*/
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

public class ServerMain {

    private static String SERVER_ADDRESS = "127.0.0.1";
    private static String MULTICAST_ADDRESS = "239.255.32.32";
    private static String REGISTRY_HOST = "localhost";
    private static int TCP_PORT = 9999;
    private static int UDP_PORT = 33333;
    private static int REG_PORT = 7777;
    private static long SOCKET_TIMEOUT = 1800000;
    private static long REWARD_TIMEOUT = 300000;
    private static long BACKUP_TIMEOUT = 120000;
    private static double AUTHOR_PERCENTAGE = 0.75;

    public static void main(String[] args) {
        File CONFIG_FILE;
        // se non viene passato alcun config_file viene avviata la
        // configurazione di default altrimenti si parsa il CONFIG_FILE
        if (args.length == 0) {
            System.out.println("SERVER: server starts with default configuration");
        } else {
            CONFIG_FILE = new File(args[0]);
            configServer(CONFIG_FILE);
        }
        System.out.print("SERVER VALUES:");
        System.out.println("\n   SERVER ADDRESS -> " + SERVER_ADDRESS + "\n   TCP PORT -> "
                + TCP_PORT + "\n   UDP PORFT ->" + UDP_PORT +
                "\n   MULTICAST ADDRESS -> " + MULTICAST_ADDRESS + "\n   REGISTRY HOST -> " + REGISTRY_HOST
                + "\n   REGISTRY PORT -> " + REG_PORT + "\n   SOCKET TIMEOUT -> " + SOCKET_TIMEOUT
                + "\n   REWARD TIMEOUT -> " + REWARD_TIMEOUT + "\n   BACKUP TIMEOUT -> " + BACKUP_TIMEOUT
                + "\n   AUTHROR PERCENTAGE -> " + AUTHOR_PERCENTAGE);

        // dichiarazione e creazione dei file per il backup
        File backupUsers = new File("..//backup//backupUsers.json");
        File backupPosts = new File("..//backup//backupPosts.json");
        try {
            backupUsers.createNewFile();
        } catch (IOException e1) {
            System.out.println("ERROR: error with backupUsers file creation");
            System.exit(-1);
        }
        try {
            backupPosts.createNewFile();
        } catch (IOException e1) {
            System.out.println("ERROR: error with backupPosts file creation");
            System.exit(-1);
        }

        // creazione social network winsome
        SocialNetwork winsome = new SocialNetwork();

        // ripristino le informazioni del social se presenti
        try {
            deserializeSocial(winsome, backupUsers, backupPosts);
        } catch (IOException e) {
            System.err.println("ERROR: error with winsome backup rebooting");
            System.exit(-1);
        }

        // avvio il thread di backup
        Backup backupThread = new Backup(winsome, backupUsers, backupPosts, BACKUP_TIMEOUT);
        backupThread.setDaemon(true);
        backupThread.start();

        // threadpool per gestire richieste dei client
        ExecutorService threadPool = Executors.newCachedThreadPool();

        // configurazione RMI
        try {
            ServerRemoteInterface stub = (ServerRemoteInterface) UnicastRemoteObject.exportObject(winsome, 0);
            LocateRegistry.createRegistry(REG_PORT);
            Registry registry = LocateRegistry.getRegistry(REG_PORT);
            registry.rebind(REGISTRY_HOST, stub);
        } catch (RemoteException e) {
            System.err.println("ERROR: error with RMI");
            System.exit(-1);
        }

        // configurazione connessione multicast
        DatagramSocket socketUDP = null;
        InetAddress multicastAddress = null;
        try {
            // creazione socket per multicast
            multicastAddress = InetAddress.getByName(MULTICAST_ADDRESS);
            socketUDP = new DatagramSocket();
        } catch (IOException e) {
            System.out.println("ERROR: problems with multicast socket creation");
            System.exit(-1);
        }

        // avvio il thread per il calcolo della ricomensa
        Reward rewardThread = new Reward(socketUDP, multicastAddress, UDP_PORT, winsome, REWARD_TIMEOUT,
                AUTHOR_PERCENTAGE);
        rewardThread.setDaemon(true);
        rewardThread.start();

        // configurazione connessioni tcp
        ServerSocket listener = null;
        try {
            listener = new ServerSocket(TCP_PORT, 70, InetAddress.getByName(SERVER_ADDRESS));
            System.out.println("SERVER: server ready on port " + TCP_PORT);
        } catch (IOException e) {
            System.out.println("ERROR: problem with server socket. Closing server");
            System.exit(-1);
        }

        // avvio il thread che si occupa della chiusura del server
        ServerCloser closerThread = new ServerCloser(listener, socketUDP, threadPool, rewardThread, backupThread);
        closerThread.setDaemon(true);
        closerThread.start();

        // Server in ascolto. Attende richiese e le inoltra al threadpool
        while (true) {
            try {
                Socket socket = listener.accept();
                socket.setSoTimeout((int) SOCKET_TIMEOUT);

                // invio dati per configurazione multicast
                DataOutputStream outWriter = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                outWriter.writeUTF(UDP_PORT + " " + MULTICAST_ADDRESS);
                outWriter.flush();

                // Gestione del client da parte di un thread del pool
                threadPool.execute(new Worker(socket, winsome));
            } catch (IOException e) {
                continue;
            }
        }
    }

    /* Metodo di configurazione del server per mezzo di un file di configurazione */
    private static void configServer(File config_file) {
        try {
            Scanner scanner = new Scanner(config_file);
            while (scanner.hasNextLine()) {
                try {
                    String line = scanner.nextLine();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        String[] split_line = line.split("=");

                        if (line.startsWith("SERVER ADDRESS"))
                            SERVER_ADDRESS = split_line[1];

                        else if (line.startsWith("TCP PORT"))
                            TCP_PORT = Integer.parseInt(split_line[1]);

                        else if (line.startsWith("UDP PORT"))
                            UDP_PORT = Integer.parseInt(split_line[1]);

                        else if (line.startsWith("MULTICAST ADDRESS"))
                            MULTICAST_ADDRESS = split_line[1];

                        else if (line.startsWith("REGISTRY HOST"))
                            REGISTRY_HOST = split_line[1];

                        else if (line.startsWith("RMI PORT"))
                            REG_PORT = Integer.parseInt(split_line[1]);

                        else if (line.startsWith("SOCKET TIMEOUT"))
                            SOCKET_TIMEOUT = Long.parseLong(split_line[1]);

                        else if (line.startsWith("REWARD TIMEOUT")) {
                            REWARD_TIMEOUT = Long.parseLong(split_line[1]);
                            if (REWARD_TIMEOUT <= 0) {
                                REWARD_TIMEOUT = 100000;
                                throw new NumberFormatException();
                            }
                        }

                        else if (line.startsWith("BACKUP TIMEOUT")) {
                            BACKUP_TIMEOUT = Long.parseLong(split_line[1]);
                            if (BACKUP_TIMEOUT <= 0) {
                                BACKUP_TIMEOUT = 120000;
                                throw new NumberFormatException();
                            }
                        }

                        else if (line.startsWith("AUTHOR PERCENTAGE")) {
                            AUTHOR_PERCENTAGE = Double.parseDouble(split_line[1]);
                            if (AUTHOR_PERCENTAGE <= 0 || AUTHOR_PERCENTAGE >= 1) {
                                AUTHOR_PERCENTAGE = 0.75;
                                throw new NumberFormatException();
                            }
                        }

                    }
                } catch (NumberFormatException e) {
                    System.out.println(
                            "SERVER: wrong parsing or wrong value of some parameters. Will be used default values for them");
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("SERVER: configuration file not found. Server starts with default configuration");
        }
    }

    /* Metodo per la deserializzazione del social network */
    private static void deserializeSocial(SocialNetwork winsome, File backupUsers, File backupPosts)
            throws IOException {
        JsonReader usersReader = new JsonReader(new InputStreamReader(new FileInputStream(backupUsers)));
        JsonReader postsReader = new JsonReader(new InputStreamReader(new FileInputStream(backupPosts)));
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if (backupPosts.length() > 0) {
            deserializePosts(winsome, postsReader, gson);
        }
        if (backupUsers.length() > 0) {
            deserializeUsers(winsome, usersReader, gson);
        }

    }

    /* Metodo per la deserializzazione dei post del social network */
    private static void deserializePosts(SocialNetwork winsome, JsonReader reader, Gson gson) throws IOException {
        ConcurrentHashMap<Long, Post> posts = new ConcurrentHashMap<>();
        Type typeOfLikes = new TypeToken<LinkedList<Vote>>() {
        }.getType();
        Type typeOfComments = new TypeToken<LinkedList<Comment>>() {
        }.getType();

        // utilizzata per tenere traccia anche dell'ultimo id utilizzato
        long id = 0;

        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            // parametri post
            String author = null;
            String title = null;
            String content = null;
            int numIter = 0;
            int numComments = 0;
            LinkedList<Vote> likes = null;
            LinkedList<Comment> comments = null;
            long lastTimeReward = 0;

            while (reader.hasNext()) {
                String next = reader.nextName();
                if (next.equals("id"))
                    id = reader.nextLong();
                else if (next.equals("author"))
                    author = reader.nextString();
                else if (next.equals("title"))
                    title = reader.nextString();
                else if (next.equals("content"))
                    content = reader.nextString();
                else if (next.equals("numIterations"))
                    numIter = reader.nextInt();
                else if (next.equals("numComments"))
                    numComments = reader.nextInt();
                else if (next.equals("votes"))
                    likes = gson.fromJson(reader.nextString(), typeOfLikes);
                else if (next.equals("comments"))
                    comments = gson.fromJson(reader.nextString(), typeOfComments);
                else if (next.equals("lastTimeReward"))
                    lastTimeReward = reader.nextLong();
                else
                    reader.skipValue();
            }
            reader.endObject();
            // controllo che almeno i valori di base siano accettabili
            if (id != 0 || author != null || title != null || content != null) {
                Post post = new Post(id, author, title, content, numIter, numComments, likes, comments, lastTimeReward);
                posts.putIfAbsent(id, post);
            }
        }
        reader.endArray();
        reader.close();
        winsome.setAllPosts(posts);
        winsome.setPostId(id);
    }

    /* Metodo per la deserializzazione degli utenti del social network */
    private static void deserializeUsers(SocialNetwork winsome, JsonReader reader, Gson gson) throws IOException {
        ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
        Type typeOfFollowAndTags = new TypeToken<LinkedList<String>>() {
        }.getType();
        Type typeOfVotes = new TypeToken<LinkedList<Long>>() {
        }.getType();
        Type typeOfBlogAndFeed = new TypeToken<Set<Long>>() {
        }.getType();

        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            // parametri utente
            String username = null;
            String hashedPassword = null;
            String seed = null;
            LinkedList<String> tags = null;
            LinkedList<String> followers = null;
            LinkedList<String> followed = null;
            LinkedList<Long> votes = null;
            ConcurrentHashMap<Long, Post> blog = new ConcurrentHashMap<>();
            ConcurrentHashMap<Long, Post> feed = new ConcurrentHashMap<>();
            Wallet wallet = null;

            while (reader.hasNext()) {
                String next = reader.nextName();
                if (next.equals("username"))
                    username = reader.nextString();
                else if (next.equals("hashedPassword"))
                    hashedPassword = reader.nextString();
                else if (next.equals("seed"))
                    seed = reader.nextString();
                else if (next.equals("tags"))
                    tags = gson.fromJson(reader.nextString(), typeOfFollowAndTags);
                else if (next.equals("followers"))
                    followers = gson.fromJson(reader.nextString(), typeOfFollowAndTags);
                else if (next.equals("followed"))
                    followed = gson.fromJson(reader.nextString(), typeOfFollowAndTags);
                else if (next.equals("listVotes"))
                    votes = gson.fromJson(reader.nextString(), typeOfVotes);
                else if (next.equals("blog")) {
                    Set<Long> set = gson.fromJson(reader.nextString(), typeOfBlogAndFeed);
                    for (Long id : set) {
                        blog.putIfAbsent(id, winsome.getPost(id));
                    }
                } else if (next.equals("feed")) {
                    Set<Long> set = gson.fromJson(reader.nextString(), typeOfBlogAndFeed);
                    for (Long id : set) {
                        feed.putIfAbsent(id, winsome.getPost(id));
                    }
                } else if (next.equals("wallet"))
                    wallet = gson.fromJson(reader.nextString(), Wallet.class);
                else
                    reader.skipValue();
            }
            reader.endObject();

            if (username != null) {
                User user = new User(username, hashedPassword, seed, tags, followers, followed, votes, blog, feed,
                        wallet);
                users.putIfAbsent(username, user);
            }
        }
        reader.endArray();
        reader.close();
        winsome.setAllUsers(users);
    }
}
