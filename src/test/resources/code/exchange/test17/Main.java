package instrument.test17;

public class Main {
    public static void main(String[] args) {
        RestService restService = new RestService();
        GithubClient client = new GithubClient(restService);
        client.fetchIssues("my-org", "my-repo");
    }
}
