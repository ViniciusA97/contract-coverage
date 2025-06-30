package instrument.test11;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class HttpClient11 {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String url;

    public HttpClient11(String url) {
        this.url = url;
    }

    public void post() {
        String postURL = String.format("%s%s", url, "/cars");
        restTemplate.exchange(postURL, HttpMethod.POST, null, String.class);
    }
}
