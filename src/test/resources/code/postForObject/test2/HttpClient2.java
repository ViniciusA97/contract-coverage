package instrument.test2;

import org.springframework.web.client.RestTemplate;

public class HttpClient2 {
    private final RestTemplate restTemplate = new RestTemplate();

    public void post() {
        this.doRequest("http://localhost:8080/test-2");
    }

    private void doRequest(String url) {
        restTemplate.postForObject(url, null, String.class);
    }
}


