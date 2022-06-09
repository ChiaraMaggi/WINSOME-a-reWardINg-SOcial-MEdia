import java.util.concurrent.atomic.AtomicLong;

public class Post {
    private final long id;
    private final String author;
    private final String title;
    private final String content;

    public Post(AtomicLong postId, String author, String title, String content) {
        this.id = postId;
        this.author = author;
        this.title = title;
        this.content = content;
    }

    // metodi getter
    public long getId() {
        return this.id;
    }

    public String getAuthor() {
        return this.author;
    }

    public String getTitle() {
        return this.title;
    }

    public String getContent() {
        return this.content;
    }

}
