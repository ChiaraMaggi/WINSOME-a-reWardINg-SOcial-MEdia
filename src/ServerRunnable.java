import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ServerRunnable implements Runnable {
    Socket clientSocket;
    SocialNetwork winsome;
    String clientUsername;

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
                        clientUsername = param[1];
                        response = "SUCCESS: " + clientUsername + " is now logged in";
                    } else {
                        response = "ERROR: wrong password for this username";
                    }
                } else {
                    response = "ERROR: user already logged in another device";
                }
            } else {
                response = "ERROR: user with this username doesn't exist";
            }
            outWriter.writeUTF(response);
            outWriter.flush();
        }

        if (param[0].equals("logout")) {
            winsome.logout(clientUsername);
            clientUsername = null;
        }

        if (param[0].equals("show")) {
            // caso con show post id
            if (param[1].equals("post")) {
                String post;
                if ((post = winsome.showPost(Long.parseLong(param[2]))) != null) {
                    response = "SUCCESS:\n" + post;
                } else {
                    response = "ERROR: no post found for that id";
                }
                outWriter.writeUTF(response);
                outWriter.flush();
            }
            // caso con show feed
            if (param[1].equals("feed")) {
                response = winsome.showFeed(clientUsername);
                outWriter.writeUTF(response);
                outWriter.flush();
            }
        }

        if (param[0].equals("comment")) {
            if (winsome.addComment(clientUsername, Long.parseLong(param[1]), param[2])) {
                response = "SUCCESS: comment created";
            } else {
                response = "ERROR: comment not created. Something goes wrong";
            }
            outWriter.writeUTF(response);
            outWriter.flush();
        }

        if (param[0].equals("blog")) {
            response = winsome.viewBlog(clientUsername);
            outWriter.writeUTF(response);
            outWriter.flush();
        }

        if (param[0].equals("post")) {
            long id;
            if ((id = winsome.createPost(clientUsername, param[1], param[2])) > 0) {
                response = "SUCCESS: post created (id = " + id + ")";
            } else {
                System.out.println(id);
                response = "ERROR: something goes wrong in creating the new post";
            }
            outWriter.writeUTF(response);
            outWriter.flush();
        }

        if (param[0].equals("follow")) {
            if (clientUsername == param[1]) {
                response = "ERROR: you can't follow yourself";
            } else {
                if (winsome.getUser(param[1]) != null) {
                    if (winsome.followUser(clientUsername, param[1])) {
                        response = "SUCCESS: you are now following user " + param[1];
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

        if (param[0].equals("delete")) {
            Long id = Long.parseLong(param[1]);
            if (winsome.getPost(id) != null) {
                if (winsome.deletePost(id, clientUsername)) {
                    response = "SUCCESS: post (id = " + id + ") deleted";
                } else {
                    response = "ERROR: you can't delete this post. You are not the author";
                }
            } else {
                response = "ERROR: post (id = " + id + ") doesn't exist";
            }
            outWriter.writeUTF(response);
            outWriter.flush();

        }

        if (param[0].equals("rate")) {
            Long id = Long.parseLong(param[1]);
            int vote = Integer.parseInt(param[2]);
            if (winsome.ratePost(id, vote, clientUsername)) {
                response = "SUCCESS: post (id = " + id + ") voted";
            } else {
                response = "ERROR: impossible to vote this post";

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
                    response = "ERROR: you can't rewin this post";
                }
            } else {
                response = "ERROR: post (id = " + id + ") doesn't exist";
            }
            outWriter.writeUTF(response);
            outWriter.flush();
        }

        if (param[0].equals("list")) {
            if (param[1].equals("users")) {
                response = winsome.listUsers(clientUsername);
                outWriter.writeUTF(response);
                outWriter.flush();
            }

            // TODO: list followers e list followed
        }
    }

}
