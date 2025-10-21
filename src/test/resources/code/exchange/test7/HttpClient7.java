package instrument.test7;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class HttpClient7 {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String url;

    public HttpClient7(String url) {
        this.url = url;
    }

    public void requestWithPathVariable(String pathVariable, HttpMethod method) {
        restTemplate.exchange(url + "/variable/" + pathVariable, method, null, String.class);
    }
}

