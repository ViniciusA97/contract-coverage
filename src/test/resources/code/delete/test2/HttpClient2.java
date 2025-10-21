package instrument.test2;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class HttpClient2 {
    private final RestTemplate restTemplate = new RestTemplate();

    public void delete() {
        this.doRequest("http://localhost:8080/test-2", HttpMethod.DELETE);
    }

    // url -> CtVariableReadImpl
    // method -> CtVariableReadImpl
    // null -> CtLiteralImpl
    // String.class -> CtFieldReadImpl
    private void doRequest(String url, HttpMethod method) {
        restTemplate.delete(url);
    }
}

