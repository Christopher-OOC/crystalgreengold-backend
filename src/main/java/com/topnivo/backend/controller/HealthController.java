package com.topnivo.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping(value = "/")
    public String healthPath() {
        return "ok";
    }

    @GetMapping(value = "/a")
    public String a() {
        return "a";
    }

    @PostMapping(value = "/b")
    public String b() {
        return "b";
    }

    }
