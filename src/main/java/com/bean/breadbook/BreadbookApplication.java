package com.bean.breadbook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class BreadbookApplication {

	public static void main(String[] args) {
		SpringApplication.run(BreadbookApplication.class, args);
	}

}
