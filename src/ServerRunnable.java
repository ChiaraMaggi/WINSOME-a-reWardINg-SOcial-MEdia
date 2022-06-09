import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ServerRunnable implements Runnable {
    Socket clientSocket;
    SocialNetwork winsome;

    public ServerRunnable(Socket socket, SocialNetwork winsome) {
        this.clientSocket = socket;
        this.winsome = winsome;
    }

    public void run() {
        String request;
        try {
            DataInputStream inReader = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream outWriter = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
            while (!Thread.currentThread().isInterrupted()) {
                request = inReader.readUTF();
                requestHandler(request, outWriter);
            }
        } catch (IOException e) {
            System.out.println("ERROR: closing client connection");
        }

    }

    private void requestHandler(String request, DataOutputStream outWriter) throws IOException {
        String[] param = request.split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        String response = null;

        if (param[0].equals("login")) {
            User user = winsome.getUser(param[1]);
            if (user != null) {
                if (!user.isLogged()) {
                    if (winsome.login(param[1], param[2])) {
                        response = "< SUCCESS: log in terminated with success";
                    } else {
                        response = "< ERROR: wrong password for this username";
                    }
                } else {
                    response = "< ERROR: user already logged in another device";
                }
            } else {
                response = "< ERROR: user with this username doesn't exist";
            }
            outWriter.writeUTF(response);
            outWriter.flush();
        }

        if (param[0].equals("logout")) {
            User user = winsome.getUser(param[1]);
            user.logout();

        }
    }

}
