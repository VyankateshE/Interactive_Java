package com.example.InteractiveDesignBackend.Controllers;

import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.InteractiveDesignBackend.Services.HtmlToDocRtfService;
import com.example.InteractiveDesignBackend.Services.LogService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HtmlToRtfController {

	@Autowired
	private HtmlToDocRtfService htmlToRtfService;

	@Autowired
	private LogService logService;

	@PostMapping("/toDoc")
	public ResponseEntity<?> convertToDoc(@RequestPart("file") MultipartFile file) {
		Date startTime = new Date();
		String randomFileName = "Doc_" + UUID.randomUUID() + ".doc";

		try {
			if (file == null || file.isEmpty()) {
				String msg = "No file provided or file is empty.";
				logService.logActivity("FAILURE", msg, startTime);
				return ResponseEntity.badRequest().body(msg);
			}

			byte[] docBytes = htmlToRtfService.convertHtmlToDoc(file.getInputStream());

			logService.logActivity("SUCCESS", "File converted successfully: " + randomFileName,
					startTime);

			return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + randomFileName)
					.contentType(MediaType.parseMediaType("application/msword")).body(docBytes);

		} catch (IllegalArgumentException e) {
			logService.logActivity("FAILURE", "Invalid input: " + e.getMessage(), startTime);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid input: " + e.getMessage());

		} catch (Exception e) {
			logService.logActivity("FAILURE", "Error converting HTML to DOC: " + e.getMessage(),
					startTime);
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error converting to DOC: " + e.getMessage());
		}
	}

	@PostMapping("/toRtf")
	public ResponseEntity<?> convertToRtf(@RequestPart("file") MultipartFile file) {
		Date startTime = new Date();
		String randomFileName = "Rtf_" + UUID.randomUUID() + ".rtf";

		try {
			if (file == null || file.isEmpty()) {
				String msg = "No file provided or file is empty.";
				logService.logActivity("FAILURE", msg, startTime);
				return ResponseEntity.badRequest().body(msg);
			}

			byte[] rtfBytes = htmlToRtfService.convertHtmlToRtf(file.getInputStream());

			logService.logActivity("SUCCESS", "File converted to RTF successfully", startTime);

			return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + randomFileName)
					.contentType(MediaType.parseMediaType("application/rtf")).body(rtfBytes);

		} catch (IllegalArgumentException e) {
			logService.logActivity("FAILURE", "Invalid input: " + e.getMessage(), startTime);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid input: " + e.getMessage());

		} catch (Exception e) {
			logService.logActivity("FAILURE", "Error converting HTML to RTF: " + e.getMessage(),
					startTime);
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error converting to RTF: " + e.getMessage());
		}
	}

}

//@PostMapping("/toDoc")
//public ResponseEntity<byte[]> convertToDoc(@RequestPart("file") MultipartFile file) throws Exception {
//byte[] docBytes = htmlToRtfService.convertHtmlToDoc(file.getInputStream());
//
//String randomFileName ="Doc_"+ UUID.randomUUID().toString() + ".doc";
//return ResponseEntity.ok()
//      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="+randomFileName)
//      .contentType(MediaType.parseMediaType("application/msword"))
//      .body(docBytes);
//}
//
//@PostMapping("/toRtf")
//public ResponseEntity<byte[]> convertToRtfs(@RequestPart("file") MultipartFile file) throws Exception {
//byte[] rtfBytes = htmlToRtfService.convertHtmlToRtf(file.getInputStream());
//
//String randomFileName = "Rtf_"+UUID.randomUUID().toString() + ".rtf";
//
//return ResponseEntity.ok()
//      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="+randomFileName)
//      .contentType(MediaType.parseMediaType("application/rtf"))
//      .body(rtfBytes);
//}
//

//@PostMapping("/uploadHtmlToRtf")
//public ResponseEntity<byte[]> uploadHtmlToRtf(
//  @RequestPart(value = "file", required = false) MultipartFile htmlFile) {
//
//Date startTime = new Date();
//
//RequestDTO requestDTO = new RequestDTO();
//
//try {
//  if (htmlFile == null || htmlFile.isEmpty()) {
//      String errorMsg = "HTML file not selected";
//      return ResponseEntity.badRequest().body(errorMsg.getBytes());
//  }
//
//  List<String> generatedRtfPaths = htmlToRtfService.processAndGenerateRtf(htmlFile);
//
//  if (generatedRtfPaths.isEmpty()) {
//      String errorMsg = "No RTF files generated from the given input";
//      return ResponseEntity.badRequest().body(errorMsg.getBytes());
//  }
//
//  String rtfPath = generatedRtfPaths.get(0);
//  File rtfFile = new File(rtfPath);
//  byte[] rtfBytes = Files.readAllBytes(rtfFile.toPath());
//
//  String randomFileName = UUID.randomUUID().toString() + ".rtf";
//
//  HttpHeaders headers = new HttpHeaders();
//  headers.setContentType(MediaType.valueOf("application/rtf"));
//  headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + randomFileName);
//
//
//  return new ResponseEntity<>(rtfBytes, headers, HttpStatus.OK);
//
//} catch (Exception e) {
//  String errorMsg = "Exception occurred: " + e.getMessage();
//  return ResponseEntity.internalServerError().body(errorMsg.getBytes());
//}
//}
