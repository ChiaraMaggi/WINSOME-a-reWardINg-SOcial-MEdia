import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static File CONFIG_FILE;

    private static String SERVER_ADDRESS = "127.0.0.1";
    private static String MULTICAST_ADDRESS = "239.255.32.32";
    private static String REGISTRY_HOST = "localhost";

    private static int TCP_SERVER_PORT = 9999;
    private static int UDP_SERVER_PORT = 33333;
    private static int MULTICAST_PORT = 44444;
    private static int RMI_PORT = 7777;
    private static int AUTHOR_PERCENTAGE = 70;
    private static int SOCKET_TIMEOUT = 100000;
    private static int REWARD_TIMEOUT = 100000;

    public static void main(String[] args) {
        // Se non viene passato alcun config_file viene avviata la
        // configurazione di default altrimenti si parsa il config_file
        if (args.length == 0) {
            System.out.println("SERVER: server stars with default configuration");
        } else {
            CONFIG_FILE = new File(args[0]);
            configServer(CONFIG_FILE);
        }

        /*
         * System.out.println("\naddress -> " + SERVER_ADDRESS + "\ntcp port -> "
         * + TCP_SERVER_PORT + "\nudp port ->" + UDP_SERVER_PORT +
         * "\nmulticast address -> "
         * + MULTICAST_ADDRESS + "\nmulticast port -> " + MULTICAST_PORT +
         * "\nresgistry host -> "
         * + REGISTRY_HOST + "\nregistry port -> " + RMI_PORT + "\nsocket timeout -> " +
         * SOCKET_TIMEOUT
         * + "\nreward timeout -> " + REWARD_TIMEOUT + "\nauthor percentage -> " +
         * AUTHOR_PERCENTAGE);
         */

        // Creazione social network winsome
        SocialNetwork winsome = new SocialNetwork();
        // Threadpool per gestire richieste dei client
        ExecutorService threadPool = Executors.newCachedThreadPool();

        // configurazione RMI
        try {
            ServerRemoteInterface stub = (ServerRemoteInterface) UnicastRemoteObject.exportObject(winsome, 0);
            LocateRegistry.createRegistry(RMI_PORT);
            Registry registry = LocateRegistry.getRegistry(RMI_PORT);
            registry.rebind(REGISTRY_HOST, stub);
        } catch (RemoteException e) {
            System.err.println("ERROR: error with RMI");
            System.exit(-1);
        }

        // Configurazione connessioni tcp
        ServerSocket listener = null;
        try {
            listener = new ServerSocket(TCP_SERVER_PORT, 50, InetAddress.getByName(SERVER_ADDRESS));
            System.out.println("SERVER: server ready on port " + TCP_SERVER_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Server in ascolto. Attende richiese e le inoltra al threadpool
        while (true) {
            try {
                Socket socket = listener.accept();
                socket.setSoTimeout(SOCKET_TIMEOUT);
                System.out.println("SERVER: new connection arrived");

                threadPool.execute(new ServerRunnable(socket, winsome));
            } catch (IOException e) {
                e.printStackTrace();
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

                        if (line.startsWith("SERVER"))
                            SERVER_ADDRESS = split_line[1];

                        else if (line.startsWith("TCPPORT"))
                            TCP_SERVER_PORT = Integer.parseInt(split_line[1]);

                        else if (line.startsWith("UDPPORT"))
                            UDP_SERVER_PORT = Integer.parseInt(split_line[1]);

                        else if (line.startsWith("MULTICAST"))
                            MULTICAST_ADDRESS = split_line[1];

                        else if (line.startsWith("MCASTPORT"))
                            MULTICAST_PORT = Integer.parseInt(split_line[1]);

                        else if (line.startsWith("REGHOST"))
                            REGISTRY_HOST = split_line[1];

                        else if (line.startsWith("REGPORT"))
                            RMI_PORT = Integer.parseInt(split_line[1]);

                        else if (line.startsWith("TIMEOUTSOCK"))
                            SOCKET_TIMEOUT = Integer.parseInt(split_line[1]);

                        else if (line.startsWith("TIMEOUTREW"))
                            REWARD_TIMEOUT = Integer.parseInt(split_line[1]);

                        else if (line.startsWith("AUTHORPERCENT"))
                            AUTHOR_PERCENTAGE = Integer.parseInt(split_line[1]);

                    }
                } catch (NumberFormatException e) {
                    System.out.println("SERVER: wrong parsing of some parameters. Use default value for them");
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("SERVER: configuration file not found. Server stars with default configuration");
        }
    }
}
