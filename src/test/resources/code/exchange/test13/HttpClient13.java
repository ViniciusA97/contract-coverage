package instrument.test13;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This class tests UriComponentsBuilder pattern with wrapper method (like metasfresh).
 * URL is built using UriComponentsBuilder and passed to a generic httpCall method.
 */
public class HttpClient13 {
    private static final String BASE_URL = "https://api.example.com/v0.1";
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Generic HTTP call wrapper that receives URI as parameter.
     */
    private String httpCall(String uri, HttpMethod method) {
        return restTemplate.exchange(uri, method, null, String.class).getBody();
    }

    public void getReaders() {
        String uri = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .path("/merchants/readers")
                .toUriString();
        httpCall(uri, HttpMethod.GET);
    }

    public void createOrder() {
        String uri = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .path("/orders")
                .toUriString();
        httpCall(uri, HttpMethod.POST);
    }

    public void deleteReader() {
        String uri = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .path("/merchants/readers/delete")
                .toUriString();
        httpCall(uri, HttpMethod.DELETE);
    }
}
