
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
    private ServerSocket socketTCP;
    private DatagramSocket socketUDP;
    private ExecutorService pool;
    private Reward reward;
    private Backup backup;

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
            socketTCP.close();
            socketUDP.close();
            reward.interrupt();
            try {
                reward.join(1000);
            } catch (InterruptedException e) {
            }
        } catch (IOException e) {
            System.out.println("ERROR: problems in closing the socket");
            System.exit(-1);
        }
        if (line.equals("closeNow")) {
            pool.shutdownNow();
        } else {
            try {
                pool.shutdown();
                pool.awaitTermination(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
            }
        }
        backup.interrupt();
        try {
            backup.savePosts();
            backup.saveUsers();
            System.out.println("SERVER: server closed");
            System.exit(0);
        } catch (IOException e) {
            System.out.println("ERROR: problem in doing the backup last time");
            System.exit(-1);
        }
        scanner.close();
    }

}
