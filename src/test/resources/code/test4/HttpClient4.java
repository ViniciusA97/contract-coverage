package instrument.test4;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

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

