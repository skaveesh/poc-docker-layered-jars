package com.skaveesh.poc.docker.layered.dockerjarsizetest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class NewController {

    @GetMapping("/test")
    public ResponseEntity<String> testMethod() {
        return new ResponseEntity<>("Layered version project ", HttpStatus.OK);
    }

}
