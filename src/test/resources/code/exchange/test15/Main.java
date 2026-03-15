package instrument.test15;

import java.net.URI;

public class Main {
    public static void main(String[] args) {
        RestService service = new RestService();
        URI uri = URI.create("http://api.example.com/v1/users");
        service.performGet(uri, String.class);
    }
}
