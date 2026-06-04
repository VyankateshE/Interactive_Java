package com.example.InteractiveDesignBackend.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.InteractiveDesignBackend.Entity.LogData;
import com.example.InteractiveDesignBackend.Repositor.LogBookRepo;

import java.util.Date;

@Service
public class LogService {

	@Autowired
	private LogBookRepo logBookRepo;

	public void logActivity(String result, String message, Date startTime) {
		try {
			LogData info = new LogData();
			info.setResult(result);
			info.setMessage(message);
			info.setSendRequestTime(startTime);
			info.setOutputResponseTime(new Date());

			logBookRepo.save(info);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Failed to log activity: " + e.getMessage());
		}
	}
}
