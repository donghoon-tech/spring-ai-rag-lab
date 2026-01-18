package com.east.springairaglab.ingestion.api;

import com.east.springairaglab.ingestion.service.IngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/api/v1/ingest")
@RequiredArgsConstructor
public class DocumentIngestionController {

    private final IngestionService ingestionService;

    @PostMapping
    public ResponseEntity<String> ingestDocuments(@RequestParam("path") String pathString) {
        log.info("Received ingestion request for path: {}", pathString);
        try {
            Path path = Paths.get(pathString);
            if (!path.toFile().exists()) {
                return ResponseEntity.badRequest().body("Path does not exist: " + pathString);
            }

            int count = ingestionService.ingest(path);
            return ResponseEntity.ok("Successfully ingested " + count + " documents from " + pathString);
        } catch (Exception e) {
            log.error("Ingestion failed", e);
            return ResponseEntity.internalServerError().body("Ingestion failed: " + e.getMessage());
        }
    }
}
