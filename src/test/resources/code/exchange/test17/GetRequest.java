package instrument.test17;

import java.util.List;
import java.util.ArrayList;

public class GetRequest {
    private final String baseURL;
    private final List<String> pathVariables;
    
    private GetRequest(Builder builder) {
        this.baseURL = builder.baseURL;
        this.pathVariables = builder.pathVariables;
    }
    
    public String getBaseURL() {
        return baseURL;
    }
    
    public List<String> getPathVariables() {
        return pathVariables;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String baseURL;
        private List<String> pathVariables = new ArrayList<>();
        
        public Builder baseURL(String baseURL) {
            this.baseURL = baseURL;
            return this;
        }
        
        public Builder pathVariables(List<String> pathVariables) {
            this.pathVariables = pathVariables;
            return this;
        }
        
        public GetRequest build() {
            return new GetRequest(this);
        }
    }
}
