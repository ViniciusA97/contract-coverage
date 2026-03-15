package instrument.test13;

/**
 * Main class that calls HttpClient13 methods to trigger endpoint detection.
 */
public class Main {
    public static void main(String[] args) {
        HttpClient13 client = new HttpClient13();
        client.getReaders();
        client.createOrder();
        client.deleteReader();
    }
}
