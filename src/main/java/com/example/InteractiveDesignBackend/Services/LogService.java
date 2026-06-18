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
	
	
	public void logException(
	        String result,
	        String message,
	        Exception exception,
	        Date startTime) {

	    try {

	        StringBuilder errorDetails =
	                new StringBuilder(message);

	        if (exception != null) {

	            errorDetails.append("\n\nException: ")
	                    .append(exception.getClass().getName());

	            errorDetails.append("\nMessage: ")
	                    .append(exception.getMessage());

	            errorDetails.append("\n\nStack Trace:\n");

	            for (StackTraceElement element :
	                    exception.getStackTrace()) {

	                errorDetails.append(element.toString())
	                        .append("\n");
	            }
	        }

	        LogData info = new LogData();

	        info.setResult(result);
	        info.setMessage(errorDetails.toString());
	        info.setSendRequestTime(startTime);
	        info.setOutputResponseTime(new Date());

	        logBookRepo.save(info);

	    } catch (Exception ex) {

	        ex.printStackTrace();
	    }
	}
}
