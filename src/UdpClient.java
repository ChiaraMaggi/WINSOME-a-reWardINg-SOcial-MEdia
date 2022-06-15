import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class UdpClient implements Runnable {

    private final MulticastSocket mcastSocket;
    private final StringBuilder notifications;

    public UdpClient(MulticastSocket mcastSocket) {
        this.mcastSocket = mcastSocket;
        notifications = new StringBuilder();
    }

    public void run() {
        byte[] buff = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buff, buff.length);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                mcastSocket.receive(packet);
                String message = new String(packet.getData(), StandardCharsets.UTF_8);
                message = message.replace("\u0000", "");
                Instant time = Instant.now();
                // le notifiche ricevute vengono archiviate all'interno della cronolgia
                synchronized (notifications) {
                    notifications.append("< ").append(time.toString(), 0, 10).append(" - ")
                            .append(time.toString(), 11, 19)
                            .append(" -> ").append(message).append("\n");
                }
            } catch (IOException e) {
                System.out.println("ERROR: problems with multicast");
                continue;
            }
        }
    }

    public void printNotifications() {
        String printable;
        synchronized (notifications) {
            printable = notifications.toString();
        }
        if (printable.isEmpty())
            System.out.println("< There are not notifications to show");
        else
            System.out.print(printable);
    }
}
