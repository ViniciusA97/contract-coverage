package instrument.test3;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class HttpClient3 {
    private final RestTemplate restTemplate = new RestTemplate();

    public void delete() {
        this.doRequest("http://localhost:8080/test-3", HttpMethod.DELETE);
    }

    // url -> CtVariableReadImpl
    // method -> CtVariableReadImpl
    // null -> CtLiteralImpl
    // String.class -> CtFieldReadImpl
    private void doRequest(String url, HttpMethod method) {
        restTemplate.delete(url);
    }
}

