package instrument.test17;

import java.util.Arrays;
import java.util.List;

public class GithubClient {
    private static final String GITHUB_BASE_URI = "https://api.github.com";
    private static final String REPOS = "repos";
    private static final String ISSUES = "issues";
    
    private final RestService restService;
    
    public GithubClient(RestService restService) {
        this.restService = restService;
    }
    
    public Object fetchIssues(String owner, String repo) {
        final List<String> pathVariables = Arrays.asList(
            REPOS,
            owner,
            repo,
            ISSUES
        );
        
        final GetRequest getRequest = GetRequest.builder()
            .baseURL(GITHUB_BASE_URI)
            .pathVariables(pathVariables)
            .build();
        
        return restService.performGet(getRequest, Object[].class).getBody();
    }
}
