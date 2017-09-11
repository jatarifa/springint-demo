package com.sensage.integration.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class App {
	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = new SpringApplication(App.class).run(args);
		Runtime.getRuntime().addShutdownHook(new Thread(ctx::close));
	}
}