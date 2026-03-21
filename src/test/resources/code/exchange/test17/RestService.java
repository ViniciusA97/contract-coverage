package instrument.test17;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.util.List;

public class RestService {
    private final RestTemplate restTemplate = new RestTemplate();

    public <T> ResponseEntity<T> performGet(GetRequest getRequest, Class<T> clazz) {
        final UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(getRequest.getBaseURL());

        List<String> pathVars = getRequest.getPathVariables();
        if (pathVars != null && !pathVars.isEmpty()) {
            uriBuilder.pathSegment(pathVars.toArray(new String[0]));
        }

        URI resourceURI = uriBuilder.build().encode().toUri();

        HttpEntity<String> request = new HttpEntity<>(null);
        return restTemplate.exchange(resourceURI, HttpMethod.GET, request, clazz);
    }
}
