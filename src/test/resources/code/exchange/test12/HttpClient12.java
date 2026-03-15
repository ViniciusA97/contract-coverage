package instrument.test12;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This class tests UriComponentsBuilder pattern with direct call.
 * URL is built using UriComponentsBuilder.fromHttpUrl().path()
 */
public class HttpClient12 {
    private static final String BASE_URL = "https://api.example.com/v1";
    private final RestTemplate restTemplate = new RestTemplate();

    public void getUsers() {
        String uri = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .path("/users")
                .toUriString();
        restTemplate.exchange(uri, HttpMethod.GET, null, String.class);
    }
}
