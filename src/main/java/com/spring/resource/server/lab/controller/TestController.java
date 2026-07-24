package com.spring.resource.server.lab.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@RestController
@RequestMapping("/api")
public class TestController {
    
    @GetMapping("/public")
    public String getMethodName() {
        return "Hello World!";
    }
    

    @GetMapping("/private")
    public String getPrivateMethodName() {
        return "Hello private";
    }

    @GetMapping("/private/admin")
    public String getMethodName5() {
        return "Hello admin";
    }
    
    

}
