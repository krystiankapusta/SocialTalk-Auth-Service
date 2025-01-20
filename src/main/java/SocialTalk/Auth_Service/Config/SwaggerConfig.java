package SocialTalk.Auth_Service.Config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Value("${email}")
    private String email;
    @Value("${github_repository}")
    private String github_repository;
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SocialTalk Auth Service API")
                        .description("Authentication and Authorization service for SocialTalk platform")
                        .version("1.0")
                        .contact(new Contact()
                                .name("Krystian")
                                .email(email))
                        .license(new License()
                                .name("Study project")
                                .url(github_repository)));
    }
}