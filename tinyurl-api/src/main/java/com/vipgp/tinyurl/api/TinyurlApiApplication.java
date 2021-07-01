package com.vipgp.tinyurl.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class TinyurlApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TinyurlApiApplication.class, args);
	}

}
