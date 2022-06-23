import java.util.LinkedList;
import java.util.List;

public class Wallet {
    private List<String> transactions;
    private double total;

    public Wallet() {
        transactions = new LinkedList<String>();
        total = 0.0;
    }

    public synchronized List<String> getTransaction() {
        return transactions;
    }

    public synchronized double getTotal() {
        return total;
    }

    public synchronized void incrementTotal(double add) {
        total += add;
    }

    public synchronized void addTransaction(String transaction) {
        transactions.add(transaction);
    }
}
