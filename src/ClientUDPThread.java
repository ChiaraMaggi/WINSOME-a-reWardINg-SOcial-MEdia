
/**
*	@file ClientUDPThread.java
*	@author Chiara Maggi 578517
*/
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;

public class ClientUDPThread implements Runnable {

    private final MulticastSocket mcastSocket;
    private boolean someonelogged = false;

    public ClientUDPThread(MulticastSocket mcastSocket) {
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
                if (someonelogged) {
                    System.out.println("\n< " + message);
                    System.out.print("> ");
                }
            } catch (IOException e) {
                System.out.println("ERROR: problems with multicast");
                continue;
            }
        }
    }

    public void login() {
        someonelogged = true;
    }

    public void logout() {
        someonelogged = false;
    }
}
