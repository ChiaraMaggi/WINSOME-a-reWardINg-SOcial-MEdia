
public class Like {
    private String author;
    private boolean vote;
    private long timestamp;

    public Like(String author, boolean vote, long timestamp) {
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
