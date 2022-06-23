/**
 * @file Vote.java
 * @author Chiara Maggi 578517
 */
public class Vote {
    private String author;
    private boolean vote;
    private long timestamp;

    public Vote(String author, boolean vote, long timestamp) {
        this.author = author;
        this.vote = vote;
        this.timestamp = timestamp;
    }

    public boolean getVote() {
        if (vote)
            return true;
        else
            return false;
    }

    public String getAuthor() {
        return author;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
