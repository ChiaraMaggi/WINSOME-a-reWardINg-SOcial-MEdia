
/**
*	@file CLientMain.java
*	@author Chiara Maggi 578517
*/
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

public class ClientMain {
    private static String SERVER_ADDRESS = "127.0.0.1";
    private static String REGISTRY_HOST = "localhost";
    private static int TCP_PORT = 9999;
    private static int REG_PORT = 7777;
    private static long SOCKET_TIMEOUT = 300000;
    // variabili per multicast, comunicate dal server
    private static int MULTICAST_PORT;
    private static String MULTICAST_ADDRESS;
    // variabile che tiene traccia se qualcuno è loggato sul client o no
    private static boolean someoneLogged = false;
    // variabile che tiene traccia dell'utenete loggato in quel momento sul client
    private static String username;
    private final static String LOGIN_ERROR_MSG = "< ERROR: no one is logged in";
    private static ServerRemoteInterface remote;
    private static Registry registry;
    private static NotifyClientInterface obj;
    private static NotifyClientInterface stub;
    // lista followers tenuta localmente
    private static LinkedList<String> followers;
    private static ReentrantLock followersLock;

    public static void main(String[] args) {
        File CONFIG_FILE;
        // se non viene passato alcun config_file viene avviata la
        // configurazione di default altrimenti si parsa il CONFIG_FILE
        if (args.length == 0) {
            System.out.println("CLIENT: client starts with default configuration");
        } else {
            CONFIG_FILE = new File(args[0]);
            configClient(CONFIG_FILE);
        }
        System.out.print("CLIENT VALUES: ");
        System.out.println("\n   SERVER ADDRESS -> " + SERVER_ADDRESS + "\n   TCP PORT -> " + TCP_PORT +
                "\n   REGISTRY HOST -> " + REGISTRY_HOST
                + "\n   REGISTRY PORT -> " + REG_PORT + "\n   SOCKET TIMEOUT -> " +
                SOCKET_TIMEOUT);

        // inizializzo lista followers tenuta localmente lato client
        followers = new LinkedList<>();
        followersLock = new ReentrantLock();

        try {
            // configurazione tcp
            Socket socket = new Socket(InetAddress.getByName(SERVER_ADDRESS), TCP_PORT);
            socket.setSoTimeout((int) SOCKET_TIMEOUT);

            // lettura e settaggio parametri multicast
            DataInputStream inReader = new DataInputStream(socket.getInputStream());
            String[] infoMulticast;
            infoMulticast = inReader.readUTF().split(" ");
            MULTICAST_PORT = Integer.parseInt(infoMulticast[0]);
            MULTICAST_ADDRESS = infoMulticast[1];

            // configurazione multicast
            MulticastSocket mcastSocket = new MulticastSocket(MULTICAST_PORT);
            InetAddress mcastAddress = InetAddress.getByName(MULTICAST_ADDRESS);

            // per permettere più legami allo stesso socket
            mcastSocket.setReuseAddress(true);
            mcastSocket.joinGroup(mcastAddress);

            // lancio il thread che sta in ascolto di notifiche di aggiornamento del wallet
            ClientUDPThread notifyReward = new ClientUDPThread(mcastSocket);
            Thread notifyRewardThread = new Thread(notifyReward);
            notifyRewardThread.setDaemon(true);
            notifyRewardThread.start();

            // configurazione RMI: registrazione con metodo remoto
            registry = LocateRegistry.getRegistry(REG_PORT);
            remote = (ServerRemoteInterface) registry.lookup(REGISTRY_HOST);

            // servizio di notifiche per aggiornamento followers
            obj = new NotifyClient(followers);
            stub = (NotifyClientInterface) UnicastRemoteObject.exportObject(obj, 0);

            System.out.println(
                    "\n---------------------------------------- WELCOME TO WINSOME ----------------------------------------");
            // gestione richieste
            requestsHandler(socket, notifyReward);
            socket.close();
            System.exit(0);
        } catch (IOException | NotBoundException e) {
            System.out.println("ERROR: connection interrupted with server");
            System.exit(-1);
        }
    }

    /* Metodo di configurazione del client per mezzo di un file di configurazione */
    private static void configClient(File config_file) {
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

                        else if (line.startsWith("REGISTRY HOST"))
                            REGISTRY_HOST = split_line[1];

                        else if (line.startsWith("REG PORT"))
                            REG_PORT = Integer.parseInt(split_line[1]);

                        else if (line.startsWith("TIMEOUT SOCKET"))
                            SOCKET_TIMEOUT = Long.parseLong(split_line[1]);

                    }
                } catch (NumberFormatException e) {
                    System.out
                            .println(
                                    "CLIENT: wrong parsing of some parameters. Will be used default values for them\n");
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("CLIENT: configuration file not found. Client starts with default configuration\n");
        }
    }

    public static boolean requestsHandler(Socket socket, ClientUDPThread notifyReward) throws IOException {
        Scanner scanner = new Scanner(System.in);
        String[] request = null;
        String line = null;
        String serverResponse = null;

        DataOutputStream outWriter = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        DataInputStream inReader = new DataInputStream(socket.getInputStream());

        do {
            System.out.print("> ");
            line = scanner.nextLine();
            request = line.split(" ");

            if (request.length < 1 || request[0] == "") {
                System.out.println("< ERROR: missing operation");
                help();
                continue;
            }

            String operation = request[0];

            switch (operation) {
                case "register":
                    // controllo notazione della richiesta
                    if (request.length < 4 || request.length > 8) {
                        System.out.println(
                                "< ERROR: wrong notation. Usage: register <username> <password> <tags> (max 5)");
                        break;
                    }
                    LinkedList<String> tags = new LinkedList<>();
                    for (int i = 3; i < request.length; i++) {
                        tags.add(request[i]);
                    }
                    if (!someoneLogged)
                        register(request[1], request[2], tags);
                    else
                        System.out.println(
                                "< ERROR: impossible to register a new user. Someone is already using this device");
                    break;

                case "login":
                    // controllo notazione della richiesta
                    if (request.length != 3) {
                        System.out.println("< ERROR: wrong notation. Usage: login <username> <password>");
                        break;
                    }
                    if (!someoneLogged) {
                        // invio richiesta al server
                        outWriter.writeUTF(line);
                        outWriter.flush();

                        // lettura risposta e stampa esito
                        serverResponse = inReader.readUTF();
                        System.out.println("< " + serverResponse);

                        // client ufficialmente in uso da un utente
                        if (serverResponse.startsWith("SUCCESS")) {
                            someoneLogged = true;
                            notifyReward.login();
                            username = request[1];
                            followers.clear();
                            followers.addAll(remote.backupFollowers(username));
                            remote.registerForCallback(stub, username);
                        }

                    } else {
                        System.out.println("< ERROR: a user is already logged in from this device");
                    }

                    break;

                case "logout":
                    // controllo notazione della richiesta
                    if (request.length != 1) {
                        System.out.println("< ERROR: wrong notation. Usage: logout");
                        break;
                    }

                    if (someoneLogged) {
                        // invio richiesta al server
                        outWriter.writeUTF(line);
                        outWriter.flush();

                        // lettura risposta e stampa esito
                        serverResponse = inReader.readUTF();
                        System.out.println("< " + serverResponse);

                        someoneLogged = false;
                        notifyReward.logout();
                        remote.unregisterForCallback(stub, username);
                        username = null;
                    } else {
                        System.out.println(LOGIN_ERROR_MSG);
                    }

                    break;

                case "list":
                    // controllo notazione della richiesta
                    if (request.length != 2
                            || (!request[1].equals("users") && !request[1].equals("followers")
                                    && !request[1].equals("following"))) {
                        System.out
                                .println(
                                        "< ERROR: wrong notation. Usage: list users or list followers or list following");
                        break;
                    }

                    if (someoneLogged) {
                        if (request[1].equals("followers")) {
                            try {
                                followersLock.lock();
                                System.out.println("< " + followers.size() + " followers:");
                                for (String s : followers) {
                                    System.out.println("<   " + s);
                                }
                            } finally {
                                followersLock.unlock();
                            }
                            break;
                        }

                        // invio richiesta al server
                        outWriter.writeUTF(line);
                        outWriter.flush();

                        System.out.println(String.format("< %-15s| ", "User") + "Tags");
                        System.out.println(
                                "< ----------------------------------------------------------------------------------------------------");
                        serverResponse = inReader.readUTF();
                        int dim = Integer.parseInt(serverResponse);

                        for (int i = 0; i < dim; i++) {
                            serverResponse = inReader.readUTF();
                            System.out.println("< " + serverResponse);
                        }
                    } else {
                        System.out.println(LOGIN_ERROR_MSG);
                    }

                    break;

                case "follow", "unfollow":
                    // controllo notazione della richiesta
                    if (request[0].equals("follow") && request.length != 2) {
                        System.out.println("< ERROR: wrong notation. Usage: follow <username>");
                        break;
                    }
                    if (request[0].equals("unfollow") && request.length != 2) {
                        System.out.println("< ERROR: wrong notation. Usage: unfollow <username>");
                        break;
                    }

                    if (someoneLogged) {
                        // invio richiesta al server
                        outWriter.writeUTF(line);
                        outWriter.flush();

                        // lettura risposta e stampa esito
                        serverResponse = inReader.readUTF();
                        System.out.println("< " + serverResponse);
                    } else {
                        System.out.println(LOGIN_ERROR_MSG);
                    }
                    break;

                case "blog":
                    // controllo notazione della richiesta
                    if (request.length != 1) {
                        System.out.println("< ERROR: wrong notation. Usage: blog");
                        break;
                    }

                    if (someoneLogged) {
                        // invio richiesta al server
                        outWriter.writeUTF(line);
                        outWriter.flush();

                        // lettura risposta e stampa esito
                        System.out.println(
                                String.format("< %-10s| ", "Id") + String.format("%-15s| ", "Author") + "Title");
                        System.out.println(
                                "< ----------------------------------------------------------------------------------------------------");
                        serverResponse = inReader.readUTF();
                        int dim2 = Integer.parseInt(serverResponse);

                        for (int i = 0; i < dim2; i++) {
                            serverResponse = inReader.readUTF();
                            System.out.println("< " + serverResponse);
                        }

                    } else {
                        System.out.println(LOGIN_ERROR_MSG);
                    }
                    break;

                case "post":
                    // controllo notazione della richiesta
                    if (request.length == 1 || !request[1].startsWith("\"")) {
                        System.out.println("< ERROR: wrong notation. Usage: post \"<title>\" \"<content>\"");
                        break;
                    }

                    // ricompongo titolo e testo e conto se ho un numero pari di virgolette
                    String str = request[1];
                    for (int i = 2; i < request.length; i++) {
                        str = str.concat(" " + request[i]);
                    }

                    char temp;
                    int occ = 0;
                    for (int i = 0; i < str.length(); i++) {
                        temp = str.charAt(i);
                        if (temp == '\"') {
                            occ++;
                        }
                    }

                    String info[] = str.split("\"");
                    if (info.length != 4 || occ != 4) {
                        System.out.println("< ERROR: wrong notation. Usage: post \"<title>\" \"<content>\"");
                        break;
                    }

                    if (info[1].length() >= 20) {
                        System.out.println("< ERROR: title too long. Max 20 characters");
                        break;
                    }

                    if (info[3].length() >= 500) {
                        System.out.println("< ERROR: content too long. Max 500 characters");
                        break;
                    }

                    if (someoneLogged) {
                        // invio richiesta al server
                        outWriter.writeUTF(line);
                        outWriter.flush();

                        // lettura risposta e stampa esito
                        serverResponse = inReader.readUTF();
                        System.out.println("< " + serverResponse);
                    } else {
                        System.out.println(LOGIN_ERROR_MSG);
                    }
                    break;

                case "show":
                    // controllo notazione della richiesta
                    if (request.length == 1) {
                        System.out.println("< ERROR: wrong notation. Usage: show feed or show post <id>");
                        break;
                    }

                    if (((request.length == 2 || request.length > 3) && request[1].equals("post"))
                            || (request.length == 3 && !request[1].equals("post"))) {
                        System.out.println("< ERROR: wrong notation. Usage: show post <id>");
                        break;
                    }

                    if ((request.length == 2 && !request[1].equals("feed"))) {
                        System.out.println("< ERROR: wrong notation. Usage: show feed");
                        break;
                    }
                    if (request.length > 2 && request[1].equals("feed")) {
                        System.out.println("< ERROR: wrong notation. Usage: show feed");
                        break;
                    }

                    if (someoneLogged) {
                        // invio richiesta al server
                        outWriter.writeUTF(line);
                        outWriter.flush();

                        if (request[1].equals("post")) {
                            // lettura risposta e stampa esito
                            serverResponse = inReader.readUTF();
                            System.out.print(serverResponse);
                        }
                        if (request[1].equals("feed")) {
                            serverResponse = inReader.readUTF();
                            int dim3 = Integer.parseInt(serverResponse);
                            System.out.println(
                                    String.format("< %-10s| ", "Id") + String.format("%-15s| ", "Author") + "Title");
                            System.out.println(
                                    "< ----------------------------------------------------------------------------------------------------");
                            for (int i = 0; i < dim3; i++) {
                                serverResponse = inReader.readUTF();
                                System.out.println("< " + serverResponse);
                            }
                        }

                    } else {
                        System.out.println(LOGIN_ERROR_MSG);
                    }
                    break;

                case "delete":
                    // controllo notazione della richiesta
                    if (request.length != 2) {
                        System.out.println("< ERROR: wrong notation. Usage: delete <idPost>");
                        break;
                    }

                    if (someoneLogged) {
                        // invio richiesta al server
                        outWriter.writeUTF(line);
                        outWriter.flush();

                        // lettura risposta e stampa esito
                        serverResponse = inReader.readUTF();
                        System.out.println("< " + serverResponse);
                    } else {
                        System.out.println(LOGIN_ERROR_MSG);
                    }
                    break;

                case "rewin":
                    // controllo notazione della richiesta
                    if (request.length != 2) {
                        System.out.println("< ERROR: wrong notation. Usage: rewin <idPost>");
                        break;
                    }

                    if (someoneLogged) {
                        // invio richiesta al server
                        outWriter.writeUTF(line);
                        outWriter.flush();

                        // lettura risposta e stampa esito
                        serverResponse = inReader.readUTF();
                        System.out.println("< " + serverResponse);
                    } else {
                        System.out.println(LOGIN_ERROR_MSG);
                    }
                    break;

                case "rate":
                    // controllo notazione della richiesta
                    if (request.length != 3) {
                        System.out.println("< ERROR: wrong notation. Usage: rate <idPost> <vote>");
                        break;
                    }
                    if (Integer.parseInt(request[2]) != 1 && Integer.parseInt(request[2]) != -1) {
                        System.out.println("< ERROR: vote must be 1 or -1");
                        break;
                    }

                    if (someoneLogged) {
                        // invio richiesta al server
                        outWriter.writeUTF(line);
                        outWriter.flush();

                        // lettura risposta e stampa esito
                        serverResponse = inReader.readUTF();
                        System.out.println("< " + serverResponse);

                        break;
                    } else {
                        System.out.println(LOGIN_ERROR_MSG);
                    }

                case "comment":
                    // Controllo notazione della richiesta
                    if (request.length <= 2 || !request[2].startsWith("\"")) {
                        System.out.println("< ERROR: wrong notation. Usage: comment <idPost> \"<comment>\"");
                        break;
                    }
                    String str2 = request[2];
                    for (int i = 3; i < request.length; i++) {
                        str2 = str2.concat(" " + request[i]);
                    }
                    char temp2;
                    int occ2 = 0;
                    for (int i = 0; i < str2.length(); i++) {
                        temp2 = str2.charAt(i);
                        if (temp2 == '\"') {
                            occ2++;
                        }
                    }
                    String info2[] = str2.split("\"");
                    if (info2.length != 2 || occ2 != 2) {
                        System.out.println("< ERROR: wrong notation. Usage: comment <idPost> \"<comment>\"");
                        break;
                    }
                    if (info2[1].length() >= 200) {
                        System.out.println("< ERROR: comment too long. Max 200 charachters");
                        break;
                    }

                    if (someoneLogged) {
                        // invio richietsa al server
                        outWriter.writeUTF(line);
                        outWriter.flush();

                        // lettura risposta e stampa esito
                        serverResponse = inReader.readUTF();
                        System.out.println("< " + serverResponse);

                    } else {
                        System.out.println(LOGIN_ERROR_MSG);
                    }
                    break;

                case "wallet":
                    // controllo notazione della richiesta
                    if (request.length > 2 || (request.length == 2 && !request[1].equals("btc"))) {
                        System.out.println("< ERROR: wrong notation. Usage: wallet or wallet btc");
                        break;
                    }

                    if (someoneLogged) {
                        // invio richiesta al server
                        outWriter.writeUTF(line);
                        outWriter.flush();

                        // richiesta = wallet
                        if (request.length == 1) {
                            serverResponse = inReader.readUTF();
                            System.out.println("< " + serverResponse);

                            int dim4 = Integer.parseInt(inReader.readUTF());
                            for (int i = 0; i < dim4; i++) {
                                serverResponse = inReader.readUTF();
                                System.out.println("< " + serverResponse);
                            }
                            break;
                        }

                        // richiesta = wallet btc
                        if (request.length == 2) {
                            serverResponse = inReader.readUTF();
                            System.out.println("< " + serverResponse);
                        }

                    } else {
                        System.out.println(LOGIN_ERROR_MSG);
                    }
                    break;

                case "help":
                    help();
                    break;

                case "quit":
                    // controllo notazione della richiesta
                    if (request.length != 1) {
                        System.out.println("< ERROR: wrong notation. Usage: quit");
                        break;
                    }
                    if (someoneLogged) {
                        outWriter.writeUTF(line);
                        outWriter.flush();
                        inReader.close();
                        outWriter.close();
                    }
                    break;

                default:
                    System.out.println("< ERROR: operation " + operation + " not permitted");
                    break;
            }

        } while (!request[0].equals("quit"));

        scanner.close();
        outWriter.close();
        inReader.close();
        socket.close();
        return true;

    }

    private static void help() {
        System.out.println("< USAGE:\n" +
                "<      register <username> <password> <tags>\n" +
                "<      login <username> <password>\n" +
                "<      logout\n" +
                "<      list user\n" +
                "<      list followers\n" +
                "<      list following\n" +
                "<      follow <user>\n" +
                "<      unfollow <user>\n" +
                "<      blog\n" +
                "<      post \"<title>\" \"<content>\"\n" +
                "<      show feed\n" +
                "<      show post <idPost>\n" +
                "<      delete <idPost>\n" +
                "<      rewin <idPost>\n" +
                "<      rate <idPost> <vote>\n" +
                "<      comment <idPost> \"<comment>\"\n" +
                "<      wallet\n" +
                "<      wallet btc\n" +
                "<      help\n" +
                "<      quit\n");
    }

    /*
     * Metodo per registrare un nuovo utente, che invoca il metodo remoto fornito da
     * ServerRemoteInterface
     */
    private static void register(String username, String password, LinkedList<String> tags) {
        try {
            remote = (ServerRemoteInterface) registry.lookup(REGISTRY_HOST);
            if (remote.register(username, password, tags)) {
                System.out.println(
                        "< SUCCESS: registration is terminated with success");
            } else {
                System.out.println("< ERROR: username already used by another user");
            }
        } catch (NotBoundException | RemoteException e) {
            System.out.println("< ERROR: registration failed");
            System.exit(-1);
        } catch (IllegalArgumentException e) {
            System.out.println("< ERROR: password must be between 8 and 16 characters");
        }
    }
}
