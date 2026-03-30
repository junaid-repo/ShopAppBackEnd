package com.management.shop.gobalusers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class GobalusersApplication {

	public static void main(String[] args) {
		SpringApplication.run(GobalusersApplication.class, args);
	}

}
