package instrument.test2;

import org.springframework.web.client.RestTemplate;

public class HttpClient2 {
    private final RestTemplate restTemplate = new RestTemplate();

    public void get() {
        this.doRequest("http://localhost:8080/test-2");
    }

    private void doRequest(String url) {
        restTemplate.getForObject(url, String.class);
    }
}


