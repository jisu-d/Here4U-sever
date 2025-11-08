package com.example.demo5;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String hello() {
        System.out.println("hello sibal11");
        return "Hello, World! 45678dsaf";
    }
}