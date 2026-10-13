package com.example.kakeibo.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppConfigController {

    private final boolean demoMode;

    public AppConfigController(@Value("${app.demo-mode:false}") boolean demoMode) {
        this.demoMode = demoMode;
    }

    @GetMapping("/api/app-config")
    public Map<String, Boolean> getConfig() {
        return Map.of("demoMode", demoMode);
    }
}
