import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;

public class Backup extends Thread {
    private SocialNetwork winsome;
    private final File backupUsers;
    private final File backupPosts;
    private final long backupTimeout;

    public Backup(SocialNetwork winsome, File backupUsers, File backupPosts, long backupTimeout) {
        this.winsome = winsome;
        this.backupUsers = backupUsers;
        this.backupPosts = backupPosts;
        this.backupTimeout = backupTimeout;
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(backupTimeout);
                savePosts();
                saveUsers();
            } catch (InterruptedException e) {
                break;
            } catch (FileNotFoundException e) {
                System.out.println("ERROR: problem with backup files");
                break;
            } catch (IOException e) {
                continue;
            }

        }

    }

    private synchronized void savePosts() throws IOException, FileNotFoundException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(backupPosts)));
        writer.setIndent("      ");
        writer.beginArray();

        for (Long id : winsome.getAllPosts().keySet()) {
            Post p = winsome.getPost(id);
            serializePost(p, backupPosts, gson, writer);
        }

        writer.endArray();
        writer.close();
    }

    private void serializePost(Post post, File backupFile, Gson gson, JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("id").value(post.getId());
        writer.name("author").value(post.getAuthor());
        writer.name("title").value(post.getTitle());
        writer.name("content").value(post.getContent());
        writer.name("positiveVotes").value(post.getPositiveVotes());
        writer.name("negativeVotes").value(post.getNegativeVotes());
        writer.name("numComment").value(post.getNumComments());

        Type typeComments = new TypeToken<ArrayList<Comment>>() {
        }.getType();

        writer.name("comments").value(gson.toJson(post.getComments(), typeComments));
        writer.name("lastTimeReward").value(post.getLastTimeReward());
        writer.endObject();
    }

    private synchronized void saveUsers() throws IOException, FileNotFoundException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(backupUsers)));

        writer.setIndent("      ");
        writer.beginArray();

        for (String s : winsome.getAllUsers().keySet()) {
            User u = winsome.getUser(s);
            serializeUser(u, backupUsers, gson, writer);
        }

        writer.endArray();
        writer.close();

    }

    private void serializeUser(User user, File backupUsers2, Gson gson, JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("username").value(user.getUsername());
        writer.name("hashedPassword").value(user.getHashedPassword());
        writer.name("seed").value(user.getSeed());

        Type typeOfFollowAndTags = new TypeToken<LinkedList<String>>() {
        }.getType();
        writer.name("tags").value(gson.toJson(user.getTags(), typeOfFollowAndTags));
        writer.name("followers").value(gson.toJson(user.getFollowers(), typeOfFollowAndTags));
        writer.name("followed").value(gson.toJson(user.getFollowed(), typeOfFollowAndTags));
        Type typeOfVotes = new TypeToken<LinkedList<Long>>() {
        }.getType();
        writer.name("votes").value(gson.toJson(user.getListVotes(), typeOfVotes));
        Type typeOfBlogAndFeed = new TypeToken<Set<Long>>() {
        }.getType();
        writer.name("blog").value(gson.toJson(user.getBlog().keySet(), typeOfBlogAndFeed));
        writer.name("feed").value(gson.toJson(user.getFeed().keySet(), typeOfBlogAndFeed));
        writer.name("wallet").value(gson.toJson(user.getWallet()));

        writer.endObject();
    }

}