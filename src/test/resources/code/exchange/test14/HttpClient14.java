package instrument.test14;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This class tests UriComponentsBuilder.pathSegment() with multiple arguments.
 * Pattern used in metasfresh: .pathSegment("merchants", "123", "readers")
 */
public class HttpClient14 {
    private static final String BASE_URL = "https://api.example.com/v0.1";
    private final RestTemplate restTemplate = new RestTemplate();

    private String httpCall(String uri, HttpMethod method) {
        return restTemplate.exchange(uri, method, null, String.class).getBody();
    }

    public void getReaders() {
        String uri = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .pathSegment("merchants", "code123", "readers")
                .build()
                .toUriString();
        httpCall(uri, HttpMethod.GET);
    }

    public void getTransactions() {
        String uri = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .pathSegment("me", "transactions")
                .build()
                .toUriString();
        httpCall(uri, HttpMethod.GET);
    }

    public void refund() {
        String uri = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .pathSegment("me", "refund", "tx123")
                .build()
                .toUriString();
        httpCall(uri, HttpMethod.POST);
    }
}
