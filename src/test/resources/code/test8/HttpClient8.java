package instrument.test8;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class HttpClient8 {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String domain = "http://localhost:8080";

    public void post() {
        this.doRequest("/template-style", HttpMethod.POST);
    }

    private void doRequest(String path, HttpMethod method) {
        String url = String.format("%s%s", domain, path);
        restTemplate.exchange(url, method, null, String.class);
    }
}

