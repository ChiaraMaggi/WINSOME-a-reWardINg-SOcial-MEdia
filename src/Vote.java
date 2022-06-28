/**
 * @file Vote.java
 * @author Chiara Maggi 578517
 */
public class Vote {
    private final String author;
    private final boolean vote;
    private final long timestamp; // data in cui è stato inserito il voto

    public Vote(String author, boolean vote, long timestamp) {
        this.author = author;
        this.vote = vote;
        this.timestamp = timestamp;
    }

    /* Metodi getter */
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
