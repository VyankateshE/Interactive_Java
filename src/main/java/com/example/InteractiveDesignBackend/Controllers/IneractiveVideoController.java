package com.example.InteractiveDesignBackend.Controllers;

import com.example.InteractiveDesignBackend.Services.InteractiveVideoService;
import com.fasterxml.jackson.databind.ObjectMapper; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/excel")
@CrossOrigin(value = "*")
public class IneractiveVideoController {

    @Autowired
    private InteractiveVideoService excelUploadService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            int uploadId = excelUploadService.saveExcelData(file);
            return ResponseEntity.ok("" + uploadId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/headers")
    public ResponseEntity<?> getHeaders(@RequestParam int uploadId) {
        try {
            Set<String> headers = excelUploadService.getFileHeadersByUploadId(uploadId);
            if (!headers.isEmpty()) {
                return ResponseEntity.ok(headers);
            } else {
                return ResponseEntity.status(404).body("❌ No headers found for upload ID " + uploadId + ".");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Error fetching headers for upload ID " + uploadId + ": " + e.getMessage());
        }
    }

    @GetMapping("/unique-values")
    public ResponseEntity<?> getUniqueValues(
            @RequestParam int uploadId,
            @RequestParam String columnName) {
        try {
            Set<String> uniqueValues = excelUploadService.getUniqueValuesForColumn(uploadId, columnName);
            if (uniqueValues != null && !uniqueValues.isEmpty()) {
                return ResponseEntity.ok(uniqueValues);
            } else {
                return ResponseEntity.status(404).body("❌ No unique values found for column '" + columnName + "' with upload ID " + uploadId + ", or column does not exist.");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Error fetching unique values: " + e.getMessage());
        }
    }

    @PostMapping(value = "/query-full-row-form/{uploadId}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> queryFullRowFormEncoded(
            @PathVariable Integer uploadId,
            @RequestParam Map<String, String> criteria) {

     
        if (criteria.containsKey("date")) {
            try {
                Instant instant = Instant.parse(criteria.get("date"));
                String formattedDate = instant.atZone(ZoneId.of("UTC"))
                        .toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"));
                criteria.put("date", formattedDate);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("❌ Invalid date format");
            }
        }

        try {
         
            List<String> targetColumns = Collections.emptyList();

            Map<String, String> result = excelUploadService.getDynamicValuesByUploadId(uploadId, criteria, targetColumns);

            if (result != null && !result.isEmpty()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(404).body("❌ No matching row found for the given criteria.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ Server error: " + e.getMessage());
        }
    }
}