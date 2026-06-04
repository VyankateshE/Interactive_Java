package com.example.InteractiveDesignBackend.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.InteractiveDesignBackend.Dto.JsonLog;
import com.example.InteractiveDesignBackend.Services.JsonLogService;
import com.example.InteractiveDesignBackend.Services.LogService;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class JsonLogController {

	@Autowired
	private JsonLogService errorLogService;

	@Autowired
	private LogService logService;

	@PostMapping
	public ResponseEntity<?> createLogs(@RequestBody Map<String, Object> payload) throws IOException {
		Date startTime = new Date();

		try {
			Object messageObj = payload.get("message");
			String level = payload.get("level") != null ? payload.get("level").toString().toUpperCase() : "INFO";

			List<String> messages;
			if (messageObj instanceof String) {
				messages = List.of((String) messageObj);
			} else if (messageObj instanceof List) {
				messages = (List<String>) messageObj;
			} else {
				logService.logActivity("FAILURE", "Invalid message format in request payload",
						startTime);
				return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Invalid message format"));
			}

			List<JsonLog> createdLogs = errorLogService.saveLogs(messages, level);

//	            logService.logActivity(
//	                    null, "CREATE_JSON_LOG", "SUCCESS",
//	                    "JSON logs successfully created (" + createdLogs.size() + " entries, level=" + level + ")",
//	                    startTime
//	            );

			return ResponseEntity.ok(Map.of("success", true, "logs", createdLogs));

		} catch (Exception e) {
			e.printStackTrace();

			logService.logActivity("FAILURE",
					"Error occurred while saving JSON logs: " + e.getMessage(), startTime);

			return ResponseEntity.internalServerError()
					.body(Map.of("success", false, "error", "Failed to create logs", "details", e.getMessage()));
		}
	}

	@GetMapping
	public ResponseEntity<?> getLogs(@RequestParam(required = false) String from,
			@RequestParam(required = false) String to) {
		Date startTime = new Date();

		try {
			List<JsonLog> logs = errorLogService.getLogs(from, to);

			logs.sort(Comparator.comparing((JsonLog log) -> log.getDate() + " " + log.getTime()).reversed());

			logService.logActivity("SUCCESS",
					"Fetched " + logs.size() + " logs (from=" + from + ", to=" + to + ")", startTime);

			return ResponseEntity.ok(Map.of("success", true, "logs", logs));

		} catch (Exception e) {
			e.printStackTrace();

			logService.logActivity("FAILURE", "Error fetching logs: " + e.getMessage(),
					startTime);

			return ResponseEntity.internalServerError()
					.body(Map.of("success", false, "error", "Failed to fetch logs", "details", e.getMessage()));
		}
	}
}

//@PostMapping
//public ResponseEntity<?> createLogs(@RequestBody Map<String, Object> payload) throws IOException {
//  Object messageObj = payload.get("message");
//  List<String> messages;
//
//  if (messageObj instanceof String) {
//      messages = List.of((String) messageObj);
//  } else if (messageObj instanceof List) {
//      messages = (List<String>) messageObj;
//  } else {
//      return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Invalid message format"));
//  }
//
//  List<JsonLog> createdLogs = errorLogService.saveLogs(messages);
//  return ResponseEntity.ok(Map.of("success", true, "logs", createdLogs));
//}
//
//@GetMapping
//public ResponseEntity<List<JsonLog>> getLogs(
//      @RequestParam(required = false) String from,
//      @RequestParam(required = false) String to
//) throws IOException {
//  List<JsonLog> logs = errorLogService.getLogs(from, to);
//  return ResponseEntity.ok(logs);
//}
