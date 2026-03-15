package instrument.test12;

/**
 * Main class that calls the HttpClient12 to trigger the endpoint detection.
 */
public class Main {
    public static void main(String[] args) {
        HttpClient12 client = new HttpClient12();
        client.getUsers();
    }
}
