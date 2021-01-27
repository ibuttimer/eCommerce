package com.example.demo;

import com.example.demo.misc.LogUtils;
import org.slf4j.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Arrays;

@EnableJpaRepositories("com.example.demo.model.persistence.repositories")
@EntityScan("com.example.demo.model.persistence")
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class SareetaApplication {

	private static final Logger logger = LogUtils.getLogger(SareetaApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(SareetaApplication.class, args);
	}

	@Bean
	CommandLineRunner init(ApplicationArguments args) {
		return runner -> logger.info(
				String.format("%nApplication starting with command-line arguments: %s.%n" +
						"To kill this application, press Ctrl + C.", Arrays.toString(args.getSourceArgs()))
		);
	}

	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
