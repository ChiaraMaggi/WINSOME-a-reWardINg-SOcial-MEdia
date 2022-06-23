import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;

public class UdpClient implements Runnable {

    private final MulticastSocket mcastSocket;

    public UdpClient(MulticastSocket mcastSocket) {
        this.mcastSocket = mcastSocket;
    }

    public void run() {
        byte[] buff = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buff, buff.length);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                mcastSocket.receive(packet);
                String message = new String(packet.getData(), StandardCharsets.UTF_8);
                message = message.replace("\u0000", "");
                System.out.println("\n< " + message);
                System.out.print("> ");
            } catch (IOException e) {
                System.out.println("ERROR: problems with multicast");
                continue;
            }
        }
    }
}
