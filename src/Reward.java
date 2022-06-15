import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public class Reward extends Thread {
    private final DatagramSocket socketUDP;
    private final InetAddress address;
    private final int port;
    private final SocialNetwork winsome;
    private final long timeoutReward;
    private double authorPercentage;

    public Reward(DatagramSocket socketUDP, InetAddress address, int port, SocialNetwork winsome, long timeoutReward,
            double authorPercentage) {
        this.socketUDP = socketUDP;
        this.address = address;
        this.port = port;
        this.winsome = winsome;
        this.timeoutReward = timeoutReward;
        this.authorPercentage = authorPercentage;
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(timeoutReward);
                computeRewards();
                // messaggio di notifica da inziare al gruppo multicast
                byte[] buff = "CLIENT: REWARDS UPDATED".getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(buff, buff.length, address, port);
                try {
                    socketUDP.send(packet);
                } catch (IOException e) {
                    System.out.println("ERROR: multicast message not send");
                    continue;
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void computeRewards() {
        ConcurrentHashMap<Long, Post> copyOfPosts = winsome.getAllPosts();
        for (Long id : copyOfPosts.keySet()) {
            double total = 0;
            Post post = copyOfPosts.get(id);
            String author = post.getAuthor();
            Set<String> curators = new TreeSet<>(); // per non avere ripetizioni
            total += postReward(post, curators);

            int numCurators = curators.size() == 0 ? 1 : curators.size();
            double curatorsReward = total * (1 - authorPercentage) / numCurators;
            double authorReward = total * authorPercentage;

            if (curatorsReward > 0) {
                for (String c : curators) {
                    winsome.getUser(c).getWallet()
                            .addTransaction(
                                    "+" + curatorsReward + ",   curator of post (id=" + post.getId() + "),   date: "
                                            + Calendar.getInstance().getTime());
                    winsome.getUser(c).getWallet().incrementTotal(curatorsReward);
                }
            }

            if (authorReward > 0) {
                winsome.getUser(author).getWallet()
                        .addTransaction("+" + authorReward + ",   author of post (id=" + post.getId() + "),   date: "
                                + Calendar.getInstance().getTime());
                winsome.getUser(author).getWallet().incrementTotal(authorReward);
            }
        }
    }

    private double postReward(Post post, Set<String> curators) {
        double total = 0;
        double votesSum = 0;
        double commenstSum = 0;
        double partialSum = 0;
        int numIter = post.addAndGetNumIter();

        List<Vote> votes = post.getVotes();
        List<Vote> filteredVotes = new LinkedList<>();
        int positiveVotes = 0;
        int negativeVotes = 0;

        // seleziono solo i like più recenti
        for (Vote v : votes) {
            if (v.getTimestamp() > post.getLastTimeReward())
                filteredVotes.add(v);
        }
        for (Vote v : filteredVotes) {
            if (v.getVote()) { // voto positivo
                positiveVotes++;
                curators.add(v.getAuthor()); // aggiungo il curatore del like ai curatori
            } else {
                negativeVotes++; // i curatori di voti negativi non ricevono ricompense
            }
        }
        // sommatoria dei voti
        votesSum = Math.log(Math.max(0, (positiveVotes - negativeVotes)) + 1);

        // selezione commenti più recenti e quanti ne hanno fatto i singoli curatori
        List<Comment> comments = post.getComments();
        List<Comment> filteredComments = new LinkedList<>();
        Set<String> filteredCommentsAuthors = new TreeSet<>();
        for (Comment c : comments) {
            if (c.getTimestamp() > post.getLastTimeReward()) {
                filteredComments.add(c);
                filteredCommentsAuthors.add(c.getAuthor());
                curators.add(c.getAuthor());
            }
        }
        for (String s : filteredCommentsAuthors) {
            int Cp = 0;
            for (Comment c : filteredComments) {
                if (c.getAuthor() == s) {
                    Cp++;
                }
            }
            partialSum += 2 / (1 + Math.pow(Math.E, -Cp + 1));
        }
        commenstSum = Math.log(partialSum + 1);
        total = (votesSum + commenstSum) / numIter;

        post.setLastTimeReward(System.nanoTime());
        return total;
    }

}