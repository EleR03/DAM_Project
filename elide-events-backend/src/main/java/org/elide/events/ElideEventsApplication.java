package org.elide.events;

import org.elide.events.spring.AppScanConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/** Spring Boot entry point. Sprint 3: no web layer; persistence through Spring Data JPA (application.properties). */
@SpringBootApplication
@Import(AppScanConfig.class)
public class ElideEventsApplication {
	public static void main(String[] args) {
		SpringApplication.run(ElideEventsApplication.class, args);
	}
}
