package org.elide.events.spring;

import java.time.Clock;
import java.util.logging.Logger;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/** Spring App Configuration class: scans domain services and defines infrastructure beans. */
@Configuration
@ComponentScan(basePackages = { "org.elide.events.domain.services" })
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
