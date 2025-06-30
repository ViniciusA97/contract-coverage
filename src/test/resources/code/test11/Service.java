package instrument.test11;

public class Service {
    private final HttpClient11 httpClient;

    public Service(HttpClient11 httpClient) {
        this.httpClient = httpClient;
    }

    public void execute() {
        httpClient.post()
    }
}