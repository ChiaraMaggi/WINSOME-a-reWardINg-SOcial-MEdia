
/**
*	@file ServerCloser.java
*	@author Chiara Maggi 578517
*/
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerCloser extends Thread {
    private final ServerSocket socketTCP;
    private final DatagramSocket socketUDP;
    private final ExecutorService pool;
    private final Reward reward;
    private final Backup backup;

    public ServerCloser(ServerSocket socketTCP, DatagramSocket socketUDP, ExecutorService pool, Reward reward,
            Backup backup) {
        this.socketTCP = socketTCP;
        this.socketUDP = socketUDP;
        this.pool = pool;
        this.reward = reward;
        this.backup = backup;
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        String line = "";
        while ("close".compareTo(line) != 0 && "closeNow".compareTo(line) != 0) {
            System.out.println("SERVER: to terminate the server type 'close' or 'closeNow'");
            System.out.print("> ");
            line = scanner.nextLine();
        }
        System.out.println("SERVER: closing server...");
        try {
            // chiusi i socket
            socketTCP.close();
            socketUDP.close();
            // interrotto il thread che calcola le ricompense
            reward.interrupt();
            try {
                reward.join(1000);
            } catch (InterruptedException e) {
            }
        } catch (IOException e) {
            System.out.println("ERROR: problems with socket closure");
            System.exit(-1);
        }
        if (line.equals("closeNow")) {
            // chiusura immediata
            pool.shutdownNow();
        } else {
            try {
                // chiusura lenta
                pool.shutdown();
                pool.awaitTermination(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
            }
        }
        backup.interrupt();
        try {
            // prima di chiudere viene svolto l'ultimo backup
            backup.savePosts();
            backup.saveUsers();
            System.out.print("SERVER: server closed");
            System.exit(0);
        } catch (IOException e) {
            System.out.println("ERROR: problems with last backup");
            System.exit(-1);
        }
        scanner.close();
    }

}
