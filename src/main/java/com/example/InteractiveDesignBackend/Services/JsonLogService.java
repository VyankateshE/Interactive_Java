package com.example.InteractiveDesignBackend.Services;

import com.example.InteractiveDesignBackend.Dto.JsonLog;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class JsonLogService {

	private static final String LOG_DIR = "errorlogs";
	private final ObjectMapper objectMapper = new ObjectMapper();

	public synchronized List<JsonLog> saveLogs(List<String> messages, String level) throws IOException {
		LocalDate today = LocalDate.now();
		String dateStr = today.toString();

		File dir = new File(LOG_DIR);
		if (!dir.exists())
			dir.mkdirs();

		File file = new File(LOG_DIR + "/" + dateStr + ".json");
		List<JsonLog> existingLogs = new ArrayList<>();

		if (file.exists()) {
			existingLogs = objectMapper.readValue(file, new TypeReference<List<JsonLog>>() {
			});
		}

		List<JsonLog> newLogs = new ArrayList<>();
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

		int nextIdStart = existingLogs.size() + 1;

		for (int i = 0; i < messages.size(); i++) {
			String timeStr = LocalTime.now().format(timeFormatter);
			String uid = dateStr + "-" + (nextIdStart + i);

			JsonLog log = new JsonLog(uid, dateStr, timeStr, messages.get(i), level != null ? level : "INFO");
			newLogs.add(log);
		}

		List<JsonLog> updatedLogs = new ArrayList<>();
		updatedLogs.addAll(newLogs);
		updatedLogs.addAll(existingLogs);

		objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, updatedLogs);

		return newLogs;
	}

	public List<JsonLog> getLogs(String fromDateStr, String toDateStr) throws IOException {
		List<JsonLog> result = new ArrayList<>();
		File dir = new File(LOG_DIR);

		if (!dir.exists())
			return result;

		if ((fromDateStr == null || fromDateStr.isEmpty()) && (toDateStr == null || toDateStr.isEmpty())) {
			File[] allFiles = dir.listFiles((d, name) -> name.endsWith(".json"));
			if (allFiles != null) {
				for (File file : allFiles) {
					List<JsonLog> fileLogs = objectMapper.readValue(file, new TypeReference<List<JsonLog>>() {
					});
					result.addAll(fileLogs);
				}
			}
			return result;
		}

		LocalDate fromDate = (fromDateStr == null || fromDateStr.isEmpty()) ? LocalDate.now()
				: LocalDate.parse(fromDateStr);

		LocalDate toDate = (toDateStr == null || toDateStr.isEmpty()) ? LocalDate.now() : LocalDate.parse(toDateStr);

		for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
			File file = new File(LOG_DIR + "/" + date.toString() + ".json");
			if (file.exists()) {
				List<JsonLog> dailyLogs = objectMapper.readValue(file, new TypeReference<List<JsonLog>>() {
				});
				result.addAll(dailyLogs);
			}
		}

		return result;
	}

}
