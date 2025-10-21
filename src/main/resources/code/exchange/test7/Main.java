package instrument.test7;

import org.springframework.http.HttpMethod;


public class Main {
    public static final String URL = "http://localhost:8080";

    public static void main(String[] args) {
        HttpClient7 httpClient = new HttpClient7(URL);
        httpClient.requestWithPathVariable("test7", HttpMethod.POST);
    }
}
