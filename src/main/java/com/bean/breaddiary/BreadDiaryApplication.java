package com.bean.breaddiary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class BreadDiaryApplication {

	public static void main(String[] args) {
		SpringApplication.run(BreadDiaryApplication.class, args);
	}

}
