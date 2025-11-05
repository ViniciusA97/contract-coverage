package instrument.test3;

import org.springframework.web.client.RestTemplate;

public class HttpClient3 {
    private final RestTemplate restTemplate = new RestTemplate();

    public void post() {
        this.doRequest("http://localhost:8080/test-3");
    }

    private void doRequest(String url) {
        restTemplate.postForObject(url, null, String.class);
    }
}


