package io.agentengine.core.common.repository.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan("io.agentengine.core")
@EnableJpaRepositories("io.agentengine.core")
public class PersistenceConfiguration {
}
