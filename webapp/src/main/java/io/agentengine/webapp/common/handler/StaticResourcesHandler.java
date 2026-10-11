package io.agentengine.webapp.common.handler;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class StaticResourcesHandler implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/index.html").addResourceLocations("/");
        registry.addResourceHandler("/assets/**").addResourceLocations("/assets/");
    }
}
