package org.elide.events.spring;

import java.time.Clock;
import java.util.logging.Logger;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Spring App Configuration class: scans the domain services and the infrastructure and defines the validator and the
 * clock. The JPA infrastructure (DataSource, EntityManagerFactory, transaction manager, Spring Data repositories) is
 * configured by Spring Boot from application.properties.
 */
@Configuration
@ComponentScan(basePackages = { "org.elide.events.domain.services", "org.elide.events.infrastructure" })
public class AppScanConfig {
	private static Logger logger = Logger.getLogger(AppScanConfig.class.getName());

	@Bean
	public Validator initValidator() {
		logger.info("... Init VALIDATION CONTEXT.");
		ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
		return validatorFactory.getValidator();
	}

	@Bean
	public Clock initClock() {
		return Clock.systemDefaultZone();
	}
}
