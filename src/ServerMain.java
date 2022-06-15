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
import java.util.ArrayList;
import java.util.HashMap;
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
    private static File CONFIG_FILE;

    private static String SERVER_ADDRESS = "127.0.0.1";
    private static String MULTICAST_ADDRESS = "239.255.32.32";
    private static String REGISTRY_HOST = "localhost";

    private static int TCP_SERVER_PORT = 9999;
    private static int UDP_SERVER_PORT = 33333;
    private static int RMI_PORT = 7777;
    private static int MULTICAST_PORT = 44444;
    private static long SOCKET_TIMEOUT = 100000;
    private static long REWARD_TIMEOUT = 60000;
    private static long BACKUP_TIMEOUT = 60000;
    private static double AUTHOR_PERCENTAGE = 0.75;

    public static void main(String[] args) {
        // Se non viene passato alcun config_file viene avviata la
        // configurazione di default altrimenti si parsa il config_file
        if (args.length == 0) {
            System.out.println("SERVER: server stars with default configuration");
        } else {
            CONFIG_FILE = new File(args[0]);
            configServer(CONFIG_FILE);
        }
        System.out.print("SERVER VALUES:");
        System.out.println("\n   SERVER ADDRESS -> " + SERVER_ADDRESS + "\n   TCP PORT -> "
                + TCP_SERVER_PORT + "\n   UDP PORFT ->" + UDP_SERVER_PORT +
                "\n   MULTICAST ADDRESS -> "
                + MULTICAST_ADDRESS + "\n   MULTICAST PORT -> " + MULTICAST_PORT +
                "\n   REGISTRY HOST -> "
                + REGISTRY_HOST + "\n   REGISTRY PORT -> " + RMI_PORT + "\n   SOCKET TIMEOUT -> " +
                SOCKET_TIMEOUT
                + "\n   REWARD TIMEOUT -> " + REWARD_TIMEOUT + "\n   BACKUP TIMEOUT -> " + BACKUP_TIMEOUT
                + "\n   AUTHROR PERCENTAGE -> " +
                AUTHOR_PERCENTAGE);

        // dichiarazione e creazione dei file per il backup
        File backupUsers = new File("..//backupServer//backupUsers.json");
        File backupPosts = new File("..//backupServer//backupPosts.json");
        try {
            backupUsers.createNewFile();
        } catch (IOException e1) {
            System.out.println("ERROR: error in creating backupUsers file");
            System.exit(-1);
        }
        try {
            backupPosts.createNewFile();
        } catch (IOException e1) {
            System.out.println("ERROR: error in creating backupUsers file");
            System.exit(-1);
        }

        // Creazione social network winsome
        SocialNetwork winsome = new SocialNetwork();
        // Ripristino le informazioni del social se presenti
        try {
            deserializeSocial(winsome, backupUsers, backupPosts);
        } catch (IOException e) {
            System.err.println("ERROR: error in rebooting winsome backup");
            System.exit(-1);
        }

        // Avvio il thread di backup
        Backup backupThread = new Backup(winsome, backupUsers, backupPosts, BACKUP_TIMEOUT);
        backupThread.setDaemon(true);
        backupThread.start();

        // Threadpool per gestire richieste dei client
        ExecutorService threadPool = Executors.newCachedThreadPool();

        // Configurazione RMI
        try {
            ServerRemoteInterface stub = (ServerRemoteInterface) UnicastRemoteObject.exportObject(winsome, 0);
            LocateRegistry.createRegistry(RMI_PORT);
            Registry registry = LocateRegistry.getRegistry(RMI_PORT);
            registry.rebind(REGISTRY_HOST, stub);
        } catch (RemoteException e) {
            System.err.println("ERROR: error with RMI");
            System.exit(-1);
        }

        // Configurazione connessione multicast
        DatagramSocket socketUDP = null;
        InetAddress multicastAddress = null;
        try {
            // creazione socket pre multicast
            multicastAddress = InetAddress.getByName(MULTICAST_ADDRESS);
            socketUDP = new DatagramSocket();
        } catch (IOException e) {
            System.out.println("ERROR: problems in creating multicast socket");
            System.exit(-1);
        }
        Reward rewardThread = new Reward(socketUDP, multicastAddress, MULTICAST_PORT, winsome, REWARD_TIMEOUT,
                AUTHOR_PERCENTAGE);
        rewardThread.setDaemon(true);
        rewardThread.start();

        // Configurazione connessioni tcp
        ServerSocket listener = null;
        try {
            listener = new ServerSocket(TCP_SERVER_PORT, 70, InetAddress.getByName(SERVER_ADDRESS));
            System.out.println("SERVER: server ready on port " + TCP_SERVER_PORT);
        } catch (IOException e) {
            System.out.println("ERROR: problem with server socket. Closing server");
            System.exit(-1);
        }

        // Avvio il thread che si occupa della chiusura del server
        ServerCloser closerThread = new ServerCloser(listener, socketUDP, threadPool, backupThread);
        closerThread.setDaemon(true);
        closerThread.start();

        // Server in ascolto. Attende richiese e le inoltra al threadpool
        while (true) {
            try {
                Socket socket = listener.accept();
                socket.setSoTimeout((int) SOCKET_TIMEOUT);

                // invio dati per configurazione multicast
                DataOutputStream outWriter = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                outWriter.writeUTF(MULTICAST_PORT + " " + MULTICAST_ADDRESS);
                outWriter.flush();

                // Faccio gestire il client da un thread del pool
                threadPool.execute(new ServerRunnable(socket, winsome));
            } catch (IOException e) {
                continue;
            }
        }
    }

    // Funzione di cofigurazione del server per mezzo di un file di configurazione
    private static void configServer(File config_file) {
        try {
            Scanner scanner = new Scanner(CONFIG_FILE);
            while (scanner.hasNextLine()) {
                try {
                    String line = scanner.nextLine();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        String[] split_line = line.split("=");

                        if (line.startsWith("SERVER ADDRESS"))
                            SERVER_ADDRESS = split_line[1];

                        else if (line.startsWith("TCP PORT"))
                            TCP_SERVER_PORT = Integer.parseInt(split_line[1]);

                        else if (line.startsWith("UDP PORT"))
                            UDP_SERVER_PORT = Integer.parseInt(split_line[1]);

                        else if (line.startsWith("MULTICAST ADDRESS"))
                            MULTICAST_ADDRESS = split_line[1];

                        else if (line.startsWith("MULTICAST PORT"))
                            MULTICAST_PORT = Integer.parseInt(split_line[1]);

                        else if (line.startsWith("REGISTRY HOST"))
                            REGISTRY_HOST = split_line[1];

                        else if (line.startsWith("RMI PORT"))
                            RMI_PORT = Integer.parseInt(split_line[1]);

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
                            "SERVER: wrong parsing or wrong value of some parameters. Use default value for them");
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("SERVER: configuration file not found. Server stars with default configuration");
        }
    }

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

    private static void deserializePosts(SocialNetwork winsome, JsonReader reader, Gson gson) throws IOException {
        ConcurrentHashMap<Long, Post> posts = new ConcurrentHashMap<>();
        Type typeOfLikes = new TypeToken<LinkedList<Like>>() {
        }.getType();
        Type typeOfComments = new TypeToken<ArrayList<Comment>>() {
        }.getType();

        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            // parametri post
            long id = 0;
            String author = null;
            String title = null;
            String content = null;
            int numIter = 0;
            int numComments = 0;
            LinkedList<Like> likes = null;
            ArrayList<Comment> comments = null;
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
                else if (next.equals("numIterazioni"))
                    numIter = reader.nextInt();
                else if (next.equals("numComments"))
                    numComments = reader.nextInt();
                else if (next.equals("likes"))
                    likes = gson.fromJson(reader.nextString(), typeOfLikes);
                else if (next.equals("comments"))
                    comments = gson.fromJson(reader.nextString(), typeOfComments);
                else if (next.equals("lastTimereward"))
                    lastTimeReward = reader.nextLong();
                else
                    reader.skipValue();
            }
            reader.endObject();
            // controllo che almeno i valori di base siano accettabili
            if (id != 0 || author != null || title != null || content != null) {
                Post post = new Post(id, author, title, content, numIter, numComments, likes, comments,
                        lastTimeReward);
                posts.putIfAbsent(id, post);
            }
        }
        reader.endArray();
        reader.close();
        winsome.setAllPosts(posts);
    }

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
            HashMap<Long, Post> blog = new HashMap<>();
            HashMap<Long, Post> feed = new HashMap<>();
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
                else if (next.equals("votes"))
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
