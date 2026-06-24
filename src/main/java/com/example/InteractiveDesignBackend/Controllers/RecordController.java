package com.example.InteractiveDesignBackend.Controllers;


import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.example.InteractiveDesignBackend.Dto.RequestDTO;
import com.example.InteractiveDesignBackend.Entity.RecordEntity;
import com.example.InteractiveDesignBackend.Services.LogService;
import com.example.InteractiveDesignBackend.Services.RecordService;
import com.lowagie.text.pdf.codec.Base64.OutputStream;


@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RecordController {

	@Autowired
	private RecordService service;

	@Autowired
	private LogService logService;


	
	@PostMapping("/uploadHtml")
	public ResponseEntity<byte[]> uploadHtml(
	        @RequestPart(value = "payload", required = false) String payload,
	        @RequestPart(value = "jsonFile", required = false) MultipartFile[] files,
	        @RequestPart(value = "file", required = false) MultipartFile htmlFile) {

	    Date startTime = new Date();

	    try {

	        if (payload == null || payload.isEmpty()) {

	            String msg = "Payload is missing or empty";

	            logService.logActivity("FAILURE", msg, startTime);

	            return ResponseEntity.badRequest().body(msg.getBytes());
	        }

	        if (files == null || files.length == 0
	                || Arrays.stream(files).allMatch(f -> f == null || f.isEmpty())) {

	            String msg = "JSON file not selected";

	            logService.logActivity("FAILURE", msg, startTime);

	            return ResponseEntity.badRequest().body(msg.getBytes());
	        }

	        if (htmlFile == null || htmlFile.isEmpty()) {

	            String msg = "HTML file not selected";

	            logService.logActivity("FAILURE", msg, startTime);

	            return ResponseEntity.badRequest().body(msg.getBytes());
	        }

	        // UPDATED
	        Map<String, byte[]> generatedHtmls =
	                service.processAndGenerateHtml(payload, files, htmlFile);

	        if (generatedHtmls.isEmpty()) {

	            String msg =
	                    "No HTML files generated from the given input";

	            logService.logActivity("FAILURE", msg, startTime);

	            return ResponseEntity.badRequest().body(msg.getBytes());
	        }

	        // UPDATED
	        byte[] zipBytes =
	                service.createZipFromFiles(generatedHtmls);

	        String randomFileName =
	                UUID.randomUUID().toString() + ".zip";

	        HttpHeaders headers = new HttpHeaders();

	        headers.setContentType(
	                MediaType.APPLICATION_OCTET_STREAM
	        );

	        headers.set(
	                HttpHeaders.CONTENT_DISPOSITION,
	                "attachment; filename=" + randomFileName
	        );

	        int count = generatedHtmls.size();

	        String successMsg =
	                count + (count == 1 ? " HTML" : " HTMLs")
	                        + " generated and zipped successfully";

	        logService.logActivity(
	                "SUCCESS",
	                successMsg,
	                startTime
	        );

	        return new ResponseEntity<>(
	                zipBytes,
	                headers,
	                HttpStatus.OK
	        );

	    } catch (Exception e) {

	        String msg =
	                "Exception occurred while uploading HTML: "
	                        + e.getMessage();

	        logService.logActivity(
	                "FAILURE",
	                msg,
	                startTime
	        );

	        return ResponseEntity.internalServerError()
	                .body(msg.getBytes());
	    }
	}
	
	@PostMapping("/uploadSinglePagePdf")
	public ResponseEntity<byte[]> SingleHtmlToPdf(
	        @RequestPart(value = "file", required = false) MultipartFile htmlFile) {

	    Date startTime = new Date();
	    RequestDTO requestDTO = new RequestDTO();

	    try {

	        if (htmlFile == null || htmlFile.isEmpty()) {

	            String errorMsg = "HTML file not selected";

	            logService.logActivity(
	                    "FAILURE",
	                    errorMsg,
	                    startTime
	            );

	            return ResponseEntity
	                    .badRequest()
	                    .body(errorMsg.getBytes());
	        }

	        byte[] pdfBytes = service.SingleHtmlToPdf(htmlFile);

	        if (pdfBytes == null || pdfBytes.length == 0) {

	            String errorMsg =
	                    "No PDF files generated from the given input";

	            service.logToDatabase(
	                    requestDTO,
	                    "FAILURE",
	                    errorMsg,
	                    startTime
	            );

	            return ResponseEntity
	                    .badRequest()
	                    .body(errorMsg.getBytes());
	        }

	        String randomFileName =
	                UUID.randomUUID().toString() + ".pdf";

	        HttpHeaders headers = new HttpHeaders();

	        headers.setContentType(MediaType.APPLICATION_PDF);

	        headers.set(
	                HttpHeaders.CONTENT_DISPOSITION,
	                "attachment; filename=" + randomFileName
	        );

	        logService.logActivity(
	                "SUCCESS",
	                "Single page PDF generated successfully",
	                startTime
	        );

	        return new ResponseEntity<>(
	                pdfBytes,
	                headers,
	                HttpStatus.OK
	        );

	    } catch (Exception e) {

	        String errorMsg =
	                "Exception occurred: " + e.getMessage();

	        return ResponseEntity
	                .internalServerError()
	                .body(errorMsg.getBytes());
	    }
	}
	
	@PostMapping("/uploadHtmlToPdf")
	public ResponseEntity<byte[]> uploadHtmlToPdf(
	        @RequestPart(value = "file", required = false)
	        MultipartFile htmlFile,

	        @RequestPart(value = "payload", required = false)
	        String payload) {

	    Date startTime = new Date();

	    try {

	        if (htmlFile == null || htmlFile.isEmpty()) {

	            String msg = "HTML file not selected";

	            logService.logActivity(
	                    "FAILURE",
	                    msg,
	                    startTime
	            );

	            return ResponseEntity
	                    .badRequest()
	                    .body(msg.getBytes());
	        }

	        if (payload == null || payload.isBlank()) {

	            String msg = "Payload missing";

	            logService.logActivity(
	                    "FAILURE",
	                    msg,
	                    startTime
	            );

	            return ResponseEntity
	                    .badRequest()
	                    .body(msg.getBytes());
	        }

	        byte[] pdfBytes =
	                service.processAndGeneratePdf(
	                        htmlFile,
	                        payload
	                );

	        if (pdfBytes == null || pdfBytes.length == 0) {

	            String msg = "No PDF generated";

	            logService.logActivity(
	                    "FAILURE",
	                    msg,
	                    startTime
	            );

	            return ResponseEntity
	                    .badRequest()
	                    .body(msg.getBytes());
	        }

	        String randomFileName =
	                UUID.randomUUID() + ".pdf";

	        HttpHeaders headers = new HttpHeaders();

	        headers.setContentType(MediaType.APPLICATION_PDF);

	        headers.set(
	                HttpHeaders.CONTENT_DISPOSITION,
	                "attachment; filename=" + randomFileName
	        );

	        logService.logActivity(
	                "SUCCESS",
	                "PDF generated",
	                startTime
	        );

	        return new ResponseEntity<>(
	                pdfBytes,
	                headers,
	                HttpStatus.OK
	        );

	    } catch (Exception e) {

	        logService.logActivity(
	                "FAILURE",
	                e.getMessage(),
	                startTime
	        );

	        return ResponseEntity
	                .internalServerError()
	                .body(e.getMessage().getBytes());
	    }
	}
	

	@PostMapping("/uploadPdf")
	public ResponseEntity<byte[]> uploadPdf(
	        @RequestPart(value = "payload", required = false) String payload,
	        @RequestPart(value = "jsonFile", required = false) MultipartFile[] files,
	        @RequestPart(value = "file", required = false) MultipartFile htmlFile) {

	    Date startTime = new Date();

	    try {

	        if (payload == null || payload.isEmpty()) {
	            payload = "[]";
	        }

	        if (files == null || files.length == 0
	                || Arrays.stream(files).allMatch(f -> f == null || f.isEmpty())) {

	            String msg = "JSON file not selected";

	            logService.logActivity("FAILURE", msg, startTime);

	            return ResponseEntity.badRequest().body(msg.getBytes());
	        }

	        if (htmlFile == null || htmlFile.isEmpty()) {

	            String msg = "HTML file not selected";

	            logService.logActivity("FAILURE", msg, startTime);

	            return ResponseEntity.badRequest().body(msg.getBytes());
	        }

	        // UPDATED
	        Map<String, byte[]> generatedPdfs =
	                service.processAndGeneratePdf(payload, files, htmlFile);

	        if (generatedPdfs.isEmpty()) {

	            String msg = "No PDF files generated from the given input";

	            logService.logActivity("FAILURE", msg, startTime);

	            return ResponseEntity.badRequest().body(msg.getBytes());
	        }

	        // UPDATED
	        byte[] zipBytes = service.createZipFromFiles(generatedPdfs);

	        String randomFileName = UUID.randomUUID().toString() + ".zip";

	        HttpHeaders headers = new HttpHeaders();

	        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

	        headers.set(
	                HttpHeaders.CONTENT_DISPOSITION,
	                "attachment; filename=" + randomFileName
	        );

	        int pdfCount = generatedPdfs.size();

	        String successMsg =
	                pdfCount + (pdfCount == 1 ? " PDF" : " PDFs")
	                        + " generated and zipped successfully";

	        logService.logActivity("SUCCESS", successMsg, startTime);

	        return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);

	    } catch (Exception e) {

	        String msg =
	                "Exception occurred while uploading PDF: "
	                        + e.getMessage();

	        logService.logActivity("FAILURE", msg, startTime);

	        return ResponseEntity.internalServerError()
	                .body(msg.getBytes());
	    }
	}
	

@PostMapping("/uploadPdf2")
public ResponseEntity<StreamingResponseBody> uploadPdf2(
        @RequestPart(value = "payload", required = false) String payload,
        @RequestPart(value = "jsonFile", required = false) MultipartFile zipFile,
        @RequestPart(value = "file", required = false) MultipartFile htmlFile) {

    try {

        final String finalPayload =
                (payload == null || payload.isEmpty())
                        ? "[]"
                        : payload;

        // Only ZIP validation
        if (zipFile == null || zipFile.isEmpty()) {

            return ResponseEntity.badRequest()
                    .body(outputStream ->
                            outputStream.write(
                                    "JSON file not selected".getBytes()
                            ));
        }

        // HTML validation
        if (htmlFile == null || htmlFile.isEmpty()) {

            return ResponseEntity.badRequest()
                    .body(outputStream ->
                            outputStream.write(
                                    "HTML file not selected".getBytes()
                            ));
        }

        StreamingResponseBody stream = outputStream -> {

            try {

                service.processZipAndGeneratePdfFast(
                        finalPayload,
                        zipFile,
                        htmlFile,
                        outputStream
                );

            } catch (Exception e) {

                e.printStackTrace();

                writeErrorZip(outputStream, e);
            }
        };
        String random = String.valueOf(System.currentTimeMillis());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=generated_pdfs_" + random + ".zip"

                )
                .header(HttpHeaders.CACHE_CONTROL, "no-transform")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(stream);

    } catch (Exception e) {

        return ResponseEntity.internalServerError()
                .body(outputStream ->
                        outputStream.write(
                                e.getMessage().getBytes()
                        ));
    }
}

private void writeErrorZip(java.io.OutputStream outputStream, Exception exception) throws IOException {

    try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {

        zos.putNextEntry(new ZipEntry("_generation_errors.txt"));

        String message =
                exception.getMessage() == null
                        ? exception.toString()
                        : exception.getMessage();

        zos.write(message.getBytes());

        zos.closeEntry();

        zos.finish();
    }
}


@PostMapping("/uploadHtml2")
public ResponseEntity<StreamingResponseBody> uploadHtml(
        @RequestPart(value = "payload", required = false) String payload,
        @RequestPart(value = "jsonFile", required = false) MultipartFile zipFile,
        @RequestPart(value = "file", required = false) MultipartFile htmlFile) {

    Date startTime = new Date();

    try {

        if (payload == null || payload.isEmpty()) {

            String msg = "Payload is missing or empty";

            logService.logActivity("FAILURE", msg, startTime);

            return ResponseEntity.badRequest()
                    .body(outputStream -> outputStream.write(msg.getBytes()));
        }

        if (zipFile == null || zipFile.isEmpty()) {

            String msg = "JSON file not selected";

            logService.logActivity("FAILURE", msg, startTime);

            return ResponseEntity.badRequest()
                    .body(outputStream -> outputStream.write(msg.getBytes()));
        }

        if (htmlFile == null || htmlFile.isEmpty()) {

            String msg = "HTML file not selected";

            logService.logActivity("FAILURE", msg, startTime);

            return ResponseEntity.badRequest()
                    .body(outputStream -> outputStream.write(msg.getBytes()));
        }

        String randomFileName =
                UUID.randomUUID().toString() + ".zip";

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.parseMediaType("application/zip")
        );

        headers.setCacheControl("no-transform");

        headers.set(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + randomFileName
        );

        StreamingResponseBody stream = outputStream ->
                {
					try {
						service.processZipAndGenerateHtmlZip(payload, zipFile, htmlFile, outputStream);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				};

        String successMsg =
                "HTML ZIP generation started successfully";

        logService.logActivity(
                "SUCCESS",
                successMsg,
                startTime
        );

        return new ResponseEntity<>(
                stream,
                headers,
                HttpStatus.OK
        );

    } catch (Exception e) {

        String msg =
                "Exception occurred while uploading HTML: "
                        + e.getMessage();

        logService.logActivity(
                "FAILURE",
                msg,
                startTime
        );

        return ResponseEntity.internalServerError()
                .body(outputStream -> outputStream.write(msg.getBytes()));
    }
}
	

}









//@PostMapping("/uploadPdf")
//public ResponseEntity<byte[]> uploadPdf(@RequestPart(value = "payload", required = false) String payload,
//		@RequestPart(value = "jsonFile", required = false) MultipartFile[] files,
//		@RequestPart(value = "file", required = false) MultipartFile htmlFile) {
//
//	Date startTime = new Date();
//
//	try {
//		
////		if (payload == null || payload.isEmpty()) {
////			String msg = "Payload is missing or empty";
////			logService.logActivity("FAILURE", msg, startTime);
////			return ResponseEntity.badRequest().body(msg.getBytes());
////		}
//		
//		if (payload == null || payload.isEmpty()) {
//		    payload = "[]";   // default empty mapping
//		}
//
//		if (files == null || files.length == 0 || Arrays.stream(files).allMatch(f -> f == null || f.isEmpty())) {
//			String msg = "JSON file not selected";
//			logService.logActivity("FAILURE", msg, startTime);
//			return ResponseEntity.badRequest().body(msg.getBytes());
//		}
//
//		if (htmlFile == null || htmlFile.isEmpty()) {
//			String msg = "HTML file not selected";
//			logService.logActivity("FAILURE", msg, startTime);
//			return ResponseEntity.badRequest().body(msg.getBytes());
//		}
//
//		List<String> generatedPdfPaths = service.processAndGeneratePdf(payload, files, htmlFile);
//
//		if (generatedPdfPaths.isEmpty()) {
//			String msg = "No PDF files generated from the given input";
//			logService.logActivity("FAILURE", msg, startTime);
//			return ResponseEntity.badRequest().body(msg.getBytes());
//		}
//
//		byte[] zipBytes = service.createZipFromFiles(generatedPdfPaths);
//		String randomFileName = UUID.randomUUID().toString() + ".zip";
//
//		HttpHeaders headers = new HttpHeaders();
//		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//		headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + randomFileName);
//
//		int pdfCount = generatedPdfPaths.size();
//		String successMsg = pdfCount + (pdfCount == 1 ? " PDF" : " PDFs") + " generated and zipped successfully";
//		logService.logActivity( "SUCCESS", successMsg, startTime);
//
//		return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);
//
//	} catch (Exception e) {
//		String msg = "Exception occurred while uploading PDF: " + e.getMessage();
//		logService.logActivity("FAILURE", msg, startTime);
//		return ResponseEntity.internalServerError().body(msg.getBytes());
//	}
//}



//@PostMapping("/uploadHtml")
//public ResponseEntity<byte[]> uploadHtml(@RequestPart(value = "payload", required = false) String payload,
//		@RequestPart(value = "jsonFile", required = false) MultipartFile[] files,
//		@RequestPart(value = "file", required = false) MultipartFile htmlFile) {
//
//	Date startTime = new Date();
//
//	try {
//		if (payload == null || payload.isEmpty()) {
//			String msg = "Payload is missing or empty";
//			logService.logActivity( "FAILURE", msg, startTime);
//			return ResponseEntity.badRequest().body(msg.getBytes());
//		}
//
//		if (files == null || files.length == 0 || Arrays.stream(files).allMatch(f -> f == null || f.isEmpty())) {
//			String msg = "JSON file not selected";
//			logService.logActivity( "FAILURE", msg, startTime);
//			return ResponseEntity.badRequest().body(msg.getBytes());
//		}
//
//		if (htmlFile == null || htmlFile.isEmpty()) {
//			String msg = "HTML file not selected";
//			logService.logActivity( "FAILURE", msg, startTime);
//			return ResponseEntity.badRequest().body(msg.getBytes());
//		}
//
//		List<String> generatedHtmlPaths = service.processAndGenerateHtml(payload, files, htmlFile);
//		if (generatedHtmlPaths.isEmpty()) {
//			String msg = "No HTML files generated from the given input";
//			logService.logActivity( "FAILURE", msg, startTime);
//			return ResponseEntity.badRequest().body(msg.getBytes());
//		}
//
//		byte[] zipBytes = service.createZipFromFiles(generatedHtmlPaths);
//		String randomFileName = UUID.randomUUID().toString() + ".zip";
//
//		HttpHeaders headers = new HttpHeaders();
//		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//		headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + randomFileName);
//
//		int count = generatedHtmlPaths.size();
//		String successMsg = count + (count == 1 ? " HTML" : " HTMLs") + " generated and zipped successfully";
//		logService.logActivity( "SUCCESS", successMsg, startTime);
//
//		return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);
//
//	} catch (Exception e) {
//		String msg = "Exception occurred while uploading HTML: " + e.getMessage();
//		logService.logActivity( "FAILURE", msg, startTime);
//		return ResponseEntity.internalServerError().body(msg.getBytes());
//	}
//}

