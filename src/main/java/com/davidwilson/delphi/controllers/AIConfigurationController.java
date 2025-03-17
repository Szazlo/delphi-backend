package com.davidwilson.delphi.controllers;

import com.davidwilson.delphi.config.AIConfiguration;
import com.davidwilson.delphi.services.AIConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai-config")
public class AIConfigurationController {

    @Autowired
    private AIConfigurationService configService;

    @GetMapping
    public ResponseEntity<List<AIConfiguration>> getAllConfigurations() {
        return ResponseEntity.ok(configService.getAllConfigurations());
    }

    @GetMapping("/active")
    public ResponseEntity<AIConfiguration> getActiveConfiguration() {
        return configService.getActiveConfiguration()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AIConfiguration> createConfiguration(@RequestBody AIConfiguration config) {
        return ResponseEntity.ok(configService.createConfiguration(config));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AIConfiguration> updateConfiguration(
            @PathVariable String id,
            @RequestBody AIConfiguration config) {
        return configService.updateConfiguration(id, config)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable String id) {
        return configService.deleteConfiguration(id) ?
                ResponseEntity.ok().build() :
                ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> setActiveConfiguration(@PathVariable String id) {
        return configService.setActiveConfiguration(id) ?
                ResponseEntity.ok().build() :
                ResponseEntity.notFound().build();
    }
} 