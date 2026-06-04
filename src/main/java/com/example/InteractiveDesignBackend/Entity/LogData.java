package com.example.InteractiveDesignBackend.Entity;

//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Entity
public class LogData {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	int id;
	private Date sendRequestTime;
	private String result;

	@Column(name = "message", columnDefinition = "TEXT")
	private String message;
	private Date outputResponseTime;
}
