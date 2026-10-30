package com.moveinsync.config.server.controller;

import com.moveinsync.config.server.domain.SettingDefinition;
import com.moveinsync.config.server.service.ConfigDefinitionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for setting definitions and global schema defaults.
 */
@RestController
@RequestMapping("/api/v1/definitions")
public class ConfigDefinitionController {

    private final ConfigDefinitionService definitionService;

    // Injects the definition management service.
    public ConfigDefinitionController(ConfigDefinitionService definitionService) {
        this.definitionService = definitionService;
    }

    // Retrieves all defined configuration settings across the entire platform.
    @GetMapping
    public ResponseEntity<List<SettingDefinition>> listDefinitions() {
        return ResponseEntity.ok(definitionService.getAllDefinitions());
    }

    // Retrieves metadata and validation rules for a specific setting key.
    @GetMapping("/{key}")
    public ResponseEntity<SettingDefinition> getDefinition(@PathVariable String key) {
        return ResponseEntity.ok(definitionService.getDefinition(key));
    }

    // Creates or updates a setting definition.
    @PostMapping
    public ResponseEntity<SettingDefinition> saveDefinition(@Valid @RequestBody SettingDefinition definition) {
        SettingDefinition saved = definitionService.saveOrUpdate(definition);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Deletes a setting definition.
    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deleteDefinition(@PathVariable String key) {
        definitionService.deleteDefinition(key);
        return ResponseEntity.noContent().build();
    }
}
