import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class Client {
    private static File CONFIG_FILE;

    private static String SERVER_ADDRESS = "127.0.0.1";
    private static String REGISTRY_HOST = "localhost";

    private static int TCP_SERVER_PORT = 9999;
    private static int RMI_PORT = 7777;
    private static int SOCKET_TIMEOUT = 100000;

    // variabile che tiene traccia se qualcuno è loggato sul client o no
    private static boolean someoneLogged = false;

    // variabile che tiene traccia dell'utenete loggato in quel momento sul client
    private static String username;
    private static String password;
    private static String LOGIN_ERROR_MSG = "< ERROR: nobody is logged in";

    private static ServerRemoteInterface remote;
    private static Registry registry;

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("CLIENT: client stars with default configuration");
        } else {
            CONFIG_FILE = new File(args[0]);
            configClient(CONFIG_FILE);
        }

        try {
            // configurazione tcp
            Socket socket = new Socket(InetAddress.getByName(SERVER_ADDRESS), TCP_SERVER_PORT);
            socket.setSoTimeout(SOCKET_TIMEOUT);

            // configurazione RMI
            registry = LocateRegistry.getRegistry(RMI_PORT);
            remote = (ServerRemoteInterface) registry.lookup(REGISTRY_HOST);

            System.out.println("--------- WELCOME TO WINSOME ----------");

            // gestione richieste
            requestsHandler(socket);

        } catch (IOException | NotBoundException e) {
            System.out.println("ERROR: connection with server failed");
            System.exit(-1);
        }

        /*
         * System.out.println("\ntcp port -> " + tcp_server_port +
         * "\nresgistry host -> " + registry_host
         * + "\nregistry port -> " + rmi_port + "\nsocket timeout -> " +
         * socket_timeout);
         */

    }

    private static void configClient(File config_file) {
        try {
            Scanner scanner = new Scanner(config_file);
            while (scanner.hasNextLine()) {
                try {
                    String line = scanner.nextLine();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        String[] split_line = line.split("=");
                        if (line.startsWith("SERVER"))
                            SERVER_ADDRESS = split_line[1];

                        else if (line.startsWith("TCPPORT"))
                            TCP_SERVER_PORT = Integer.parseInt(split_line[1]);

                        else if (line.startsWith("REGHOST"))
                            REGISTRY_HOST = split_line[1];

                        else if (line.startsWith("REGPORT"))
                            RMI_PORT = Integer.parseInt(split_line[1]);

                        else if (line.startsWith("TIMEOUTSOCK"))
                            SOCKET_TIMEOUT = Integer.parseInt(split_line[1]);

                    }
                } catch (NumberFormatException e) {
                    System.out.println("CLIENT: wrong parsing of some parameters. Use default value for them\n");
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("CLIENT: configuration file not found. Client stars with default configuration\n");
        }
    }

    public static boolean requestsHandler(Socket socket) throws IOException {
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
                    if (request.length < 4 || request.length > 8) {
                        System.out.println(
                                "< ERROR: wrong notation. Usage: register <username> <password> <tags> (max 5)");
                        break;
                    }
                    String tags = request[3];
                    for (int i = 4; i < request.length; i++) {
                        tags = tags.concat(" " + request[i]);
                    }
                    if (!someoneLogged)
                        register(request[1], request[2], tags);
                    else
                        System.out.println(
                                "< ERROR: impossible to register a new user. Someone is already using this device");
                    break;

                case "login":
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
                            username = request[1];
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

                        System.out.println("< SUCCESS: logout teminated with success");
                        someoneLogged = false;
                        username = null;
                    } else {
                        System.out.println(LOGIN_ERROR_MSG);
                    }

                    break;

                case "list":
                    if (request.length != 2
                            || (!request[1].equals("users") && !request[1].equals("followers")
                                    && !request[1].equals("following"))) {
                        System.out
                                .println(
                                        "< ERROR: wrong notation. Usage: list users or list followers or list following");
                        break;
                    }

                    // invio richiesta al server
                    outWriter.writeUTF(line);
                    outWriter.flush();

                    break;

                case "follow", "unfollow":
                    if (request[0].equals("follow") && request.length != 2) {
                        System.out.println("< ERROR: wrong notation. Usage: follow <username>");
                        break;
                    }
                    if (request[0].equals("unfollow") && request.length != 2) {
                        System.out.println("< ERROR: wrong notation. Usage: unfollow <username>");
                        break;
                    }
                    break;

                case "blog":
                    if (someoneLogged) {
                        // controllo notazione della richiesta
                        if (request.length != 1) {
                            System.out.println("< ERROR: wrong notation. Usage: blog");
                            break;
                        }

                        // invio richiesta al server
                        outWriter.writeUTF(line);
                        outWriter.flush();

                        serverResponse = inReader.readUTF();
                        System.out.println("< Id      | Author        | Title     ");
                        System.out.println("< -----------------------------------------------------");
                        System.out.println(serverResponse);

                    } else {
                        System.out.println(LOGIN_ERROR_MSG);
                    }
                    break;

                case "post":
                    if (someoneLogged) {
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

                        outWriter.writeUTF(line);
                        outWriter.flush();

                        serverResponse = inReader.readUTF();
                        System.out.println("< " + serverResponse);
                    } else {
                        System.out.println(LOGIN_ERROR_MSG);
                    }
                    break;

                case "show":
                    if (someoneLogged) {
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

                        if ((request.length == 2 && !request[1].equals("feed"))
                                || (request.length > 2 && request[1].equals("feed"))) {
                            System.out.println("< ERROR: wrong notation. Usage: show feed");
                            break;
                        }

                        // invio richiesta al server
                        outWriter.writeUTF(line);
                        outWriter.flush();

                        serverResponse = inReader.readUTF();
                        System.out.print(serverResponse);

                    } else {
                        System.out.println(LOGIN_ERROR_MSG);
                    }
                    break;

                case "delete":
                    if (request.length != 2) {
                        System.out.println("< ERROR: wrong notation. Usage: delete <idPost>");
                        break;
                    }
                    break;

                case "rewin":
                    if (request.length != 2) {
                        System.out.println("< ERROR: wrong notation. Usage: rewin <idPost>");
                        break;
                    }
                    break;

                case "rate":
                    if (someoneLogged) {
                        // controllo notazione della richiesta
                        if (request.length != 3) {
                            System.out.println("< ERROR: wrong notation. Usage: rate <idPost> <vote>");
                            break;
                        }
                        if (Integer.parseInt(request[2]) != 1 || Integer.parseInt(request[2]) != -1) {
                            System.out.println("< ERROR: vote must be 1 or -1");
                            break;
                        }

                        // invio richiesta al server

                        break;
                    } else {
                        System.out.println(LOGIN_ERROR_MSG);
                    }

                case "comment":
                    if (someoneLogged) {
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

                        // invio richietsa al server
                        outWriter.writeUTF(line);
                        outWriter.flush();

                        serverResponse = inReader.readUTF();
                        System.out.println("< " + serverResponse);

                    } else {
                        System.out.println(LOGIN_ERROR_MSG);
                    }
                    break;

                case "wallet":
                    if (request.length > 2 || (request.length == 2 && !request[1].equals("btc"))) {
                        System.out.println("< ERROR: wrong notation. Usage: wallet or wallet btc");
                        break;
                    }
                    break;

                case "help":
                    help();
                    break;

                case "quit":
                    if (request.length != 1) {
                        System.out.println("< ERROR: wrong notation. Usage: quit");
                        break;
                    }
                    break;

                default:
                    System.out.println("< ERROR: operation " + operation + " not permitted");
                    break;
            }

        } while (!request[0].equals("quit"));

        scanner.close();
        return true;

    }

    private static void help() {
        System.out.println("< USAGE:\n" +
                "   -> register <username> <password> <tags>\n" +
                "   -> login <username> <password>\n" +
                "   -> logout\n" +
                "   -> list user\n" +
                "   -> list followers\n" +
                "   -> list following\n" +
                "   -> follow <user>\n" +
                "   -> unfollow <user>\n" +
                "   -> blog\n" +
                "   -> post \"<title>\" \"<content>\"\n" +
                "   -> show feed\n" +
                "   -> show post <idPost>\n" +
                "   -> delete <idPost>\n" +
                "   -> rewin <idPost>\n" +
                "   -> rate <idPost> <vote>\n" +
                "   -> comment <idPost> \"<comment>\"\n" +
                "   -> wallet\n" +
                "   -> wallet btc\n" +
                "   -> quit\n");
    }

    private static void register(String username, String password, String tags) {
        try {
            remote = (ServerRemoteInterface) registry.lookup(REGISTRY_HOST);
            if (remote.register(username, password, tags)) {
                System.out.println(
                        "< SUCCESS: registration is terminated with success. Remember to log in before starting");
            } else {
                System.out.println("< ERROR: username already used by another user");
            }
        } catch (NotBoundException | RemoteException e) {
            System.out.println("< ERROR: registration faild");
            System.exit(-1);
        } catch (IllegalArgumentException e) {
            System.out.println("< ERROR: password must be between 8 and 16 characters");
        }

    }

}
