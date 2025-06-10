package instrument.test4;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

/**
 * This class is used to represent a simple HTTP Client. In our test, this API call need to be registred with a
 * Endpoint(path="/test-4", method="POST"), Endpoint(path="/", method="GET"), Endpoint(path="/", method="PUT") because
 * is called in another point of the code (in the same client).
 */
public class HttpClient4 {
    private final RestTemplate restTemplate = new RestTemplate();

    public void post() {
        this.doRequest("http://localhost:8080/test-4", HttpMethod.POST);
    }
    public void get() {
        this.doRequest("http://localhost:8080", HttpMethod.GET);
    }
    public void put() {
        this.doRequest("http://localhost:8080", HttpMethod.PUT);
    }

    private void doRequest(String url, HttpMethod method) {
        restTemplate.exchange(url, method, null, String.class);
    }
}

