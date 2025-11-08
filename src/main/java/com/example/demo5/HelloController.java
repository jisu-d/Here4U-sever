package com.example.demo5;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable; // PathVariable 임포트
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping("/")
    public String hello() {
        System.out.println("hello root");
        return "Hello, World!";
    }

    @GetMapping("/{name}")
    public String helloName(@PathVariable String name) {
        System.out.println(name);
        return "Hello, World! " + name;
    }
}