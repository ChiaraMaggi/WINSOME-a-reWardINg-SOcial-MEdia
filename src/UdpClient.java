import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

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
                // le notifiche ricevute vengono archiviate all'interno della cronolgia
                synchronized (notifications) {
                    notifications.append("< ").append(Calendar.getInstance().getTime())
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
