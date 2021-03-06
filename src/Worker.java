
/**
*	@file Worker.java
*	@author Chiara Maggi 578517
*/
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class Worker implements Runnable {
    private final Socket clientSocket;
    private final SocialNetwork winsome;
    private String clientUsername;

    public Worker(Socket socket, SocialNetwork winsome) {
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
            try {
                // server chiude la connessione col client perchè non arrivano nuove richieste
                winsome.getUser(clientUsername).logout();
                clientSocket.close();
            } catch (NullPointerException ex2) {
                try {
                    clientSocket.close();
                } catch (IOException e1) {
                    // ignore
                }
            } catch (IOException ex) {
                System.err.println("ERROR: problems with clientSocket closure");
            }
        }
    }

    /* Metodo per la gestione delle richieste che arrivano dal Client */
    private void requestHandler(String request, DataOutputStream outWriter) throws IOException {
        String[] param = request.split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        String response = null;

        if (param[0].equals("login")) {
            User user = winsome.getUser(param[1]);
            if (user != null) {
                if (!user.isLogged()) {
                    if (winsome.login(param[1], param[2])) {
                        clientUsername = param[1];
                        response = "SUCCESS: " + clientUsername + " is logged in";
                    } else {
                        response = "ERROR: wrong password";
                    }
                } else {
                    response = "ERROR: user already logged in on another device";
                }
            } else {
                response = "ERROR: user " + param[1] + " does not exist";
            }
            outWriter.writeUTF(response);
            outWriter.flush();
        }

        if (param[0].equals("logout")) {
            winsome.logout(clientUsername);
            clientUsername = null;
            response = "SUCCESS: logout teminated with success";
            outWriter.writeUTF(response);
            outWriter.flush();
        }

        if (param[0].equals("list")) {
            if (param[1].equals("users")) {
                response = winsome.listUsers(clientUsername);
                String[] listUsers = response.split("\n");
                Integer dim = listUsers.length;

                // invio del numero di stringhe che verranno inviate
                outWriter.writeUTF(dim.toString());
                outWriter.flush();

                // invio delle singole stringhe
                for (int i = 0; i < dim; i++) {
                    response = listUsers[i];
                    outWriter.writeUTF(response);
                    outWriter.flush();
                }
            }

            if (param[1].equals("following")) {
                response = winsome.listFollowing(clientUsername);
                String[] listFollowing = response.split("\n");
                Integer dim = listFollowing.length;

                // invio del numero di stringhe che verranno inviate
                outWriter.writeUTF(dim.toString());
                outWriter.flush();

                // invio delle singole stringhe
                for (int i = 0; i < dim; i++) {
                    response = listFollowing[i];
                    outWriter.writeUTF(response);
                    outWriter.flush();
                }
            }
        }

        if (param[0].equals("follow")) {
            if (clientUsername == param[1]) {
                response = "ERROR: you can not follow yourself";
            } else {
                if (winsome.getUser(param[1]) != null) {
                    if (winsome.followUser(clientUsername, param[1])) {
                        response = "SUCCESS: you started following user " + param[1];
                    } else {
                        response = "ERROR: user " + param[1] + " already followed";
                    }
                } else {
                    response = "ERROR: the user you want to follow does not exist";
                }
            }
            outWriter.writeUTF(response);
            outWriter.flush();
        }

        if (param[0].equals("unfollow")) {
            if (clientUsername == param[1]) {
                response = "ERROR: you can't unfollow yourself";
            } else {
                if (winsome.getUser(param[1]) != null) {
                    if (winsome.unfollowUser(clientUsername, param[1])) {
                        response = "SUCCESS: you have unfollowed user " + param[1];
                    } else {
                        response = "ERROR: user " + param[1] + " already not followed";
                    }
                } else {
                    response = "ERROR: the user you want to unfollow does not exist";
                }
            }
            outWriter.writeUTF(response);
            outWriter.flush();
        }

        if (param[0].equals("blog")) {
            response = winsome.viewBlog(clientUsername);
            String[] blog = response.split("\n");
            Integer dim = blog.length;

            // invio del numero di stringhe che verranno inviate
            outWriter.writeUTF(dim.toString());
            outWriter.flush();

            // invio delle singole stringhe
            for (int i = 0; i < dim; i++) {
                response = blog[i];
                outWriter.writeUTF(response);
                outWriter.flush();
            }
        }

        if (param[0].equals("post")) {
            long id;
            if ((id = winsome.createPost(clientUsername, param[1], param[2])) > 0) {
                response = "SUCCESS: post created (id = " + id + ")";
            } else {
                response = "ERROR: post not creating. Something goes wrong";
            }
            outWriter.writeUTF(response);
            outWriter.flush();
        }

        if (param[0].equals("show")) {
            // caso con show post id
            if (param[1].equals("post")) {
                String post;
                if ((post = winsome.showPost(Long.parseLong(param[2]))) != null) {
                    response = "SUCCESS:\n" + post;
                } else {
                    response = "< ERROR: no post found with id = " + param[2] + "\n";
                }
                outWriter.writeUTF(response);
                outWriter.flush();
            }
            // caso con show feed
            if (param[1].equals("feed")) {
                response = winsome.showFeed(clientUsername);
                String[] feed = response.split("\n");
                Integer dim = feed.length;

                // invio del numero di stringhe che verranno inviate
                outWriter.writeUTF(dim.toString());
                outWriter.flush();

                // invio delle singole stringhe
                for (int i = 0; i < dim; i++) {
                    response = feed[i];
                    outWriter.writeUTF(response);
                    outWriter.flush();
                }
            }
        }

        if (param[0].equals("delete")) {
            Long id = Long.parseLong(param[1]);
            if (winsome.getPost(id) != null) {
                if (winsome.deletePost(id, clientUsername)) {
                    response = "SUCCESS: post (id = " + id + ") deleted";
                } else {
                    response = "ERROR: you can't delete this post. You are not the author";
                }
            } else {
                response = "ERROR: post (id = " + id + ") does not exist";
            }
            outWriter.writeUTF(response);
            outWriter.flush();
        }

        if (param[0].equals("rewin")) {
            Long id = Long.parseLong(param[1]);
            if (winsome.getPost(id) != null) {
                if (winsome.rewinPost(id, clientUsername)) {
                    response = "SUCCESS: post (id = " + id + ") rewinned";
                } else {
                    response = "ERROR: impossible to rewin this post. Maybe you are the author or this post is not in your feed";
                }
            } else {
                response = "ERROR: post (id = " + id + ") does not exist";
            }
            outWriter.writeUTF(response);
            outWriter.flush();
        }

        if (param[0].equals("rate")) {
            Long id = Long.parseLong(param[1]);
            int vote = Integer.parseInt(param[2]);
            if (winsome.ratePost(id, vote, clientUsername)) {
                response = "SUCCESS: post (id = " + id + ") rated";
            } else {
                response = "ERROR: impossible to vote this post. Maybe yo have already vote or you are the author or this post is not in your feed";

            }
            outWriter.writeUTF(response);
            outWriter.flush();
        }

        if (param[0].equals("comment")) {
            if (winsome.addComment(clientUsername, Long.parseLong(param[1]), param[2])) {
                response = "SUCCESS: comment created";
            } else {
                response = "ERROR: comment not created. Maybe you are the author or this post is not in your feed";
            }
            outWriter.writeUTF(response);
            outWriter.flush();
        }

        if (param[0].equals("wallet")) {
            if (param.length == 1) {
                Wallet wallet = winsome.getUser(clientUsername).getWallet();
                response = "Total (wincoin): " + wallet.getTotal();
                outWriter.writeUTF(response);
                outWriter.flush();

                Integer dim = wallet.getTransactions().size();
                // invio del numero di stringhe che verranno inviate
                outWriter.writeUTF(dim.toString());
                outWriter.flush();

                List<String> transactions = wallet.getTransactions();
                // invio delle singole stringhe
                for (String s : transactions) {
                    outWriter.writeUTF("    " + s);
                    outWriter.flush();
                }
            }

            if (param.length == 2) {
                try {
                    Wallet wallet = winsome.getUser(clientUsername).getWallet();
                    response = "Total (bitcoin): " + winsome.toBitcoin(wallet.getTotal());
                } catch (IOException e) {
                    response = "ERROR: problems with the wallet calculation. Try again later";
                }
                outWriter.writeUTF(response);
                outWriter.flush();
            }
        }

        if (param[0].equals("quit")) {
            winsome.getUser(clientUsername).logout();
        }
    }

}
