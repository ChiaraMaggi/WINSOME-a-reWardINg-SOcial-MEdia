import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class Reward extends Thread {
    private final DatagramSocket socketUDP;
    private final InetAddress address;
    private final int port;
    private final SocialNetwork winsome;
    private final long timeoutReward;
    private double authorPercentage;

    public Reward(DatagramSocket socketUDP, InetAddress address, int port, SocialNetwork winsome, long timeoutReward,
            double authorPercentage) {
        this.socketUDP = socketUDP;
        this.address = address;
        this.port = port;
        this.winsome = winsome;
        this.timeoutReward = timeoutReward;
        this.authorPercentage = authorPercentage;
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(timeoutReward);
                computeRewards();
                // messaggio di notifica da inziare al gruppo multicast
                byte[] buff = "CLIENT: REWARDS UPDATED".getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(buff, buff.length, address, port);
                try {
                    socketUDP.send(packet);
                } catch (IOException e) {
                    System.out.println("ERROR: multicast message not send");
                    continue;
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void computeRewards() {

    }

}
