
/**
*	@file Backup.java
*	@author Chiara Maggi 578517
*/
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
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

    public synchronized void savePosts() throws IOException, FileNotFoundException {
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

        Type typeOfLikes = new TypeToken<LinkedList<Vote>>() {
        }.getType();
        Type typeOfComments = new TypeToken<LinkedList<Comment>>() {
        }.getType();

        writer.name("id").value(post.getId());
        writer.name("author").value(post.getAuthor());
        writer.name("title").value(post.getTitle());
        writer.name("content").value(post.getContent());
        writer.name("numIterations").value(post.getNumIter());
        writer.name("numComments").value(post.getNumComments());
        try {
            post.votesLock();
            writer.name("votes").value(gson.toJson(post.getVotes(), typeOfLikes));
        } finally {
            post.votesUnlock();
        }
        try {
            post.commentsLock();
            writer.name("comments").value(gson.toJson(post.getComments(), typeOfComments));
        } finally {
            post.commentsUnlock();
        }
        writer.name("lastTimeReward").value(post.getLastTimeReward());

        writer.endObject();
    }

    public synchronized void saveUsers() throws IOException, FileNotFoundException {
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

    private void serializeUser(User user, File backupUsers, Gson gson, JsonWriter writer) throws IOException {
        writer.beginObject();

        Type typeOfFollowAndTags = new TypeToken<LinkedList<String>>() {
        }.getType();
        Type typeOfVotes = new TypeToken<LinkedList<Long>>() {
        }.getType();
        Type typeOfBlogAndFeed = new TypeToken<Set<Long>>() {
        }.getType();

        writer.name("username").value(user.getUsername());
        writer.name("hashedPassword").value(user.getHashedPassword());
        writer.name("seed").value(user.getSeed());
        writer.name("tags").value(gson.toJson(user.getTags(), typeOfFollowAndTags));
        try {
            user.followersLock();
            writer.name("followers").value(gson.toJson(user.getFollowers(), typeOfFollowAndTags));
        } finally {
            user.followersUnlock();
        }
        writer.name("followed").value(gson.toJson(user.getFollowed(), typeOfFollowAndTags));
        writer.name("listVotes").value(gson.toJson(user.getListVotes(), typeOfVotes));
        writer.name("blog").value(gson.toJson(user.getBlog().keySet(), typeOfBlogAndFeed));
        writer.name("feed").value(gson.toJson(user.getFeed().keySet(), typeOfBlogAndFeed));
        writer.name("wallet").value(gson.toJson(user.getWallet()));

        writer.endObject();
    }

}
