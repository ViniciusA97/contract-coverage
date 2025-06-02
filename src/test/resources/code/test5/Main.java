package instrument.test5;

import org.springframework.http.HttpMethod;


public class Main {
    public static final String URL = "http://localhost:8080";

    public static void main(String[] args) {
        HttpClient5 httpClient = new HttpClient5();
        httpClient.doRequest(URL +"/test-5", HttpMethod.POST);
    }
}
