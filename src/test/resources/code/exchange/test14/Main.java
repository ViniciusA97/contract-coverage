package instrument.test14;

/**
 * Main class that calls HttpClient14 methods to trigger endpoint detection.
 */
public class Main {
    public static void main(String[] args) {
        HttpClient14 client = new HttpClient14();
        client.getReaders();
        client.getTransactions();
        client.refund();
    }
}
