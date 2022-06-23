
/**
*	@file Post.java
*	@author Chiara Maggi 578517
*/
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Post {
    private final long id;
    private final String author;
    private final String title;
    private final String content;
    private int numIter;
    private int numComments;
    private final LinkedList<Vote> votes;
    private final LinkedList<Comment> comments;
    private long lastTimeReward;

    private final ReentrantLock votesLock;
    private final ReentrantLock commentsLock;

    public Post(long postId, String author, String title, String content) {
        this.id = postId;
        this.author = author;
        this.title = title;
        this.content = content;
        numIter = 0;
        numComments = 0;
        votes = new LinkedList<>();
        comments = new LinkedList<>();
        lastTimeReward = 0;
        votesLock = new ReentrantLock();
        commentsLock = new ReentrantLock();
    }

    public Post(long postId, String author, String title, String content, int numIter,
            int numComments, LinkedList<Vote> votes, LinkedList<Comment> comments, long lastTimeReward) {
        this.id = postId;
        this.author = author;
        this.title = title;
        this.content = content;
        this.numIter = numIter;
        this.numComments = numComments;
        this.votes = votes;
        this.comments = comments;
        this.lastTimeReward = lastTimeReward;
        votesLock = new ReentrantLock();
        commentsLock = new ReentrantLock();
    }

    // metodi getter
    public long getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public int getNumIter() {
        return numIter;
    }

    public void votesLock() {
        votesLock.lock();
    }

    public void votesUnlock() {
        votesLock.unlock();
    }

    public void commentsLock() {
        commentsLock.lock();
    }

    public void commentsUnlock() {
        commentsLock.unlock();
    }

    public int getPositiveVotes() {
        int positiveVotes = 0;
        for (Vote v : votes) {
            if (v.getVote()) {
                positiveVotes++;
            }
        }
        return positiveVotes;
    }

    public int getNegativeVotes() {
        int negativeVotes = 0;
        for (Vote v : votes) {
            if (!v.getVote()) {
                negativeVotes++;
            }
        }
        return negativeVotes;
    }

    public int getNumComments() {
        return numComments;
    }

    public List<Vote> getVotes() {
        return votes;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public String getCommentsInString() {
        String str = "";
        for (int i = 0; i < comments.size(); i++) {
            str = str.concat("<    " + comments.get(i).getAuthor() + ": " + comments.get(i).getComment() + "\n");
        }
        return str;
    }

    public Long getLastTimeReward() {
        return lastTimeReward;
    }

    public int addAndGetNumIter() {
        numIter++;
        return numIter;
    }

    // metodi add
    public void addComment(String username, String contentComment) {
        Comment comment = new Comment(username, contentComment, System.nanoTime());
        comments.add(comment);
        numComments++;
    }

    public void addVote(String username, int vote) {
        Vote v;
        if (vote > 0) {
            v = new Vote(username, true, System.nanoTime());
        } else {
            v = new Vote(username, false, System.nanoTime());
        }
        votes.add(v);
    }

    public void setLastTimeReward(long timestamp) {
        lastTimeReward = timestamp;
    }

}
