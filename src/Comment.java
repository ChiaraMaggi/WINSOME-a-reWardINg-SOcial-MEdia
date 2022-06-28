/**
 * @file Comment.java
 * @author Chiara Maggi 578517
 */
public class Comment {
    private final String author;
    private final String comment;
    private final long timestamp;

    public Comment(String author, String comment, long timestamp) {
        this.author = author;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    /* Metodi getter */
    public String getAuthor() {
        return author;
    }

    public String getComment() {
        return comment;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
