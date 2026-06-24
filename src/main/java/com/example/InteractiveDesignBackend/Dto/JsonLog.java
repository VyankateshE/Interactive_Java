package com.example.InteractiveDesignBackend.Dto;

import lombok.Data;

@Data
public class JsonLog {
	private String uid;
	private String date;
	private String time;
	private String message;
	private String level;

	public JsonLog() {
	}

	public JsonLog(String uid, String date, String time, String message, String level) {
		this.uid = uid;
		this.date = date;
		this.time = time;
		this.message = message;
		this.level = level;
	}
}
