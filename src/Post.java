import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Post {
    private final long id;
    private final String author;
    private final String title;
    private final String content;

    private int numIter;
    private int numComments;

    private final LinkedList<Like> likes;
    private final ArrayList<Comment> comments;

    private long lastTimeReward;

    public Post(long postId, String author, String title, String content) {
        this.id = postId;
        this.author = author;
        this.title = title;
        this.content = content;
        numIter = 0;
        numComments = 0;
        likes = new LinkedList<>();
        comments = new ArrayList<>();
        lastTimeReward = 0;
    }

    public Post(long postId, String author, String title, String content, int numIter,
            int numComments, LinkedList<Like> likes, ArrayList<Comment> comments, long lastTimeReward) {
        this.id = postId;
        this.author = author;
        this.title = title;
        this.content = content;
        this.numIter = numIter;
        this.numComments = numComments;
        this.likes = likes;
        this.comments = comments;
        this.lastTimeReward = lastTimeReward;
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

    public int getNumIter() {
        return numIter;
    }

    public int getPositiveVotes() {
        int positiveVotes = 0;
        for (Like l : likes) {
            if (l.getVote()) {
                positiveVotes++;
            }
        }
        return positiveVotes;
    }

    public int getNegativeVotes() {
        int negativeVotes = 0;
        for (Like l : likes) {
            if (!l.getVote()) {
                negativeVotes++;
            }
        }
        return negativeVotes;
    }

    public int getNumComments() {
        return numComments;
    }

    public List<Like> getLikes() {
        return likes;
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

    // metodi add
    public void addComment(String username, String contentComment) {
        Comment comment = new Comment(username, contentComment);
        comments.add(comment);
        numComments++;
    }

    public void addLike(String username, int vote) {
        Like like;
        if (vote > 0) {
            like = new Like(username, true, System.nanoTime());
        } else {
            like = new Like(username, false, System.nanoTime());
        }
        likes.add(like);
    }

}
