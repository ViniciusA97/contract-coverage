package instrument.test18;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;

public class RestService {
    private static final String BASE_URL = "https://api.example.com";
    private final RestTemplate restTemplate = new RestTemplate();

    public <T> ResponseEntity<T> performGet(String path, Class<T> clazz) {
        // Chained call - should work
        URI resourceURI = UriComponentsBuilder.fromHttpUrl(BASE_URL).path(path).build().toUri();

        HttpEntity<String> request = new HttpEntity<>(null);
        return restTemplate.exchange(resourceURI, HttpMethod.GET, request, clazz);
    }
}
