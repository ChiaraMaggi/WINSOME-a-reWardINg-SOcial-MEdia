import java.util.ArrayList;
import java.util.List;

public class Post {
    private final long id;
    private final String author;
    private final String title;
    private final String content;

    private int positiveVotes;
    private int negativeVotes;

    private int numComments;

    private final ArrayList<Comment> comments;

    private long lastTimeReward;

    public Post(long postId, String author, String title, String content) {
        this.id = postId;
        this.author = author;
        this.title = title;
        this.content = content;
        positiveVotes = 0;
        negativeVotes = 0;
        numComments = 0;
        comments = new ArrayList<>();
        lastTimeReward = 0;
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

    public int getPositiveVotes() {
        return positiveVotes;
    }

    public int getNegativeVotes() {
        return negativeVotes;
    }

    public int getNumComments() {
        return numComments;
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

    public void addComment(String username, String contentComment) {
        Comment comment = new Comment(username, contentComment);
        comments.add(comment);
        numComments++;
    }

    public void addPostiveVote() {
        positiveVotes++;
    }

    public void addNegativeVotes() {
        negativeVotes++;
    }
}
