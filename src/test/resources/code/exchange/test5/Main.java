package instrument.test5;

import org.springframework.http.HttpMethod;

/**
 * This class is used to represent a object that call a Client Wrapper. In our test, this API call need to be registred
 * as Endpoint(path="/test-5", metbod="POST").
 */
public class Main {
    public static final String URL = "http://localhost:8080";

    public static void main(String[] args) {
        HttpClient5 httpClient = new HttpClient5();
        httpClient.doRequest(URL +"/test-5", HttpMethod.POST);
    }
}
