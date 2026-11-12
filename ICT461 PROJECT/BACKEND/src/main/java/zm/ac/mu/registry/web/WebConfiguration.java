package zm.ac.mu.registry.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    private final RateLimitInterceptor rateLimit;
    private final JsonOnlyInterceptor jsonOnly;
    private final SessionInterceptor session;
    private final RoleInterceptor roles;

    public WebConfiguration(RateLimitInterceptor rateLimit, JsonOnlyInterceptor jsonOnly,
            SessionInterceptor session, RoleInterceptor roles) {
        this.rateLimit = rateLimit;
        this.jsonOnly = jsonOnly;
        this.session = session;
        this.roles = roles;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimit).addPathPatterns("/api/v1/**");
        registry.addInterceptor(jsonOnly).addPathPatterns("/api/v1/**");
        registry.addInterceptor(session).addPathPatterns("/api/v1/**");
        registry.addInterceptor(roles).addPathPatterns("/api/v1/**");
    }
}
