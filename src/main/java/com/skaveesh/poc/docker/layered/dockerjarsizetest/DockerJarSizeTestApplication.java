package com.skaveesh.poc.docker.layered.dockerjarsizetest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages={"com.example.dockerjarsizetest.controller"})
public class DockerJarSizeTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(DockerJarSizeTestApplication.class, args);
	}

}
