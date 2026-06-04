package com.example.InteractiveDesignBackend.Controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.example.InteractiveDesignBackend.FileToMultipartFile;
import com.example.InteractiveDesignBackend.FileUploadResponse;
import com.example.InteractiveDesignBackend.Mypath;
import com.example.InteractiveDesignBackend.Dto.RequestDTO;
import com.example.InteractiveDesignBackend.Entity.LogData;
import com.example.InteractiveDesignBackend.Entity.User;
import com.example.InteractiveDesignBackend.Repositor.LogBookRepo;
import com.example.InteractiveDesignBackend.Repositor.Repository;
import com.example.InteractiveDesignBackend.Services.JsonService;
import com.example.InteractiveDesignBackend.Services.LogService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class Controller {

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	Repository repository;

	@Autowired
	JsonService serviceLogic;

	@Autowired
	LogBookRepo logBookRepo;

	@Autowired
	private LogService logService;
	
//	private static final Logger LOGGER = Logger.getLogger(Controller.class);

	private static final Logger LOGGER =
	        LoggerFactory.getLogger(Controller.class);
	
	@GetMapping("/getData")
public ResponseEntity<String> getData() {
        String response = "Hello the api is running!!";
        return ResponseEntity.ok(response);
}


	@GetMapping("/show-html/{id}")
	public String showHtmlById(@PathVariable("id") Integer id) {
		String basePath = "C:\\Users\\Ariantech 01\\eclipse-workspace\\InteractiveDesignLatest-main\\htmlFilePath\\HtmlDownloads\\";
		String filePath = basePath + id + "_downloadabless_html.html"; // Example: 1_editable_html.html,
																		// 2_editable_html.html, etc.

		try {
			byte[] bytes = Files.readAllBytes(Paths.get(filePath));
			String htmlContent = new String(bytes);
			return htmlContent;

		} catch (IOException e) {
			e.printStackTrace();
			return "<h1>Error: File not found for ID: " + id + "</h1>";
		}
	}

	@PostMapping("/uploadFile/{id}")
	public ResponseEntity<?> uploadFile(@PathVariable("id") Integer id,
			@RequestParam(value = "editableHtml", required = false) MultipartFile editableHtml,
			@RequestParam(value = "downloadableHtml", required = false) MultipartFile downloadableHtml) {

		Date startTime = new Date();

		try {
			Optional<User> optionalUser = repository.findById(id);
			if (!optionalUser.isPresent()) {
				String errorMessage = "User not found with id: " + id;
				logService.logActivity("FAILURE", errorMessage, startTime);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
			}

			User user = optionalUser.get();

			if (editableHtml != null && !editableHtml.isEmpty()) {
				String folderPath = checkFolder("HtmlDownloads");
				String editableFileName = generateFileName(user.getId(), "editable_html.html");
				saveFile(folderPath + File.separator + editableFileName, editableHtml);
				user.setEditableHtml(folderPath + File.separator + editableFileName);
			}

			if (downloadableHtml != null && !downloadableHtml.isEmpty()) {
				String folderPath = checkFolder("HtmlDownloads");
				String downloadableFileName = generateFileName(user.getId(), "downloadable_html.html");
				saveFile(folderPath + File.separator + downloadableFileName, downloadableHtml);
				user.setDownloadableHtml(folderPath + File.separator + downloadableFileName);
			}

			repository.save(user);

			String successMessage = "HTML files updated successfully for user ID " + id;
			logService.logActivity("SUCCESS", successMessage, startTime);

			FileUploadResponse response = new FileUploadResponse();
			response.setFileName1(user.getEditableHtml());
			response.setFileName2(user.getDownloadableHtml());
			response.setMessage(successMessage);

			return ResponseEntity.ok(response);

		} catch (IOException e) {
			String errorMessage = "File processing error: " + e.getMessage();
			e.printStackTrace();
			logService.logActivity("FAILURE", errorMessage, startTime);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);

		} catch (Exception e) {
			String errorMessage = "Unexpected error: " + e.getMessage();
			e.printStackTrace();
			logService.logActivity("FAILURE", errorMessage, startTime);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
		}
	}

	@PostMapping("/uploadFile")
	public ResponseEntity<?> uploadFile(
			@RequestParam(value = "editableHtml", required = false) MultipartFile editableHtml,
			@RequestParam(value = "downloadableHtml", required = false) MultipartFile downloadableHtml,
			@RequestParam(value = "name", required = false) String name) {

		Date startTime = new Date();
		try {
			if (ObjectUtils.isEmpty(name)) {
				String msg = "Name is required.";
				logService.logActivity("FAILURE", msg, startTime);
				return ResponseEntity.badRequest().body(msg);
			}

			if (editableHtml == null || editableHtml.isEmpty()) {
				String msg = "Editable HTML file is missing.";
				logService.logActivity("FAILURE", msg, startTime);
				return ResponseEntity.badRequest().body(msg);
			}

			if (downloadableHtml == null || downloadableHtml.isEmpty()) {
				String msg = "Downloadable HTML file is missing.";
				logService.logActivity("FAILURE", msg, startTime);
				return ResponseEntity.badRequest().body(msg);
			}

			if (doesNameExist(name)) {
				String msg = "Template name already exists: " + name;
				logService.logActivity("FAILURE", msg, startTime);
				return ResponseEntity.badRequest().body(msg);
			}

			User user = new User();
			user.setName(name);
			user = repository.save(user);

			String folderPath = checkFolder("HtmlDownloads");

			String editableFileName = generateFileName(user.getId(), "editable_html.html");
			saveFile(editableFileName, editableHtml);
			user.setEditableHtml(folderPath + File.separator + editableFileName);

			String downloadableFileName = generateFileName(user.getId(), "downloadable_html.html");
			saveFile(downloadableFileName, downloadableHtml);
			user.setDownloadableHtml(folderPath + File.separator + downloadableFileName);

			repository.save(user);

			String successMsg = "HTML templates saved with name: " + name;
			logService.logActivity("SUCCESS", successMsg, startTime);

			FileUploadResponse response = new FileUploadResponse();
			response.setFileName1(user.getEditableHtml());
			response.setFileName2(user.getDownloadableHtml());
			response.setMessage(successMsg);

			return ResponseEntity.ok(response);

		} catch (IOException e) {
			String msg = "File processing error: " + e.getMessage();
			e.printStackTrace();
			logService.logActivity("FAILURE", msg, startTime);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);

		} catch (Exception e) {
			String msg = "Unexpected error during file upload: " + e.getMessage();
			e.printStackTrace();
			logService.logActivity("FAILURE", msg, startTime);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
		}
	}

	private String generateFileName(Integer userId, String fileType) {
		return userId + "_" + fileType;
	}

	private void saveFile(String fileName, MultipartFile multipartFile) throws IOException {
		String folderPath = checkFolder("HtmlDownloads");
		Path filePath = Paths.get(folderPath).resolve(fileName);
		try (InputStream inputStream = multipartFile.getInputStream()) {
			Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private String checkFolder(String folder) throws IOException {

		String currentDirectory = System.getProperty("user.dir");

		Path uploadPath = Paths.get(currentDirectory, folder);

		Path folderPath = Files.createDirectories(uploadPath);

		return folderPath.toString();

	}

	public boolean doesNameExist(String name) {
		Optional<User> item = repository.findByName(name);
		return item.isPresent();
	}

	// not using
	@GetMapping("/getFile")
	public ResponseEntity<?> getById(@RequestBody RequestDTO requestDTO) {
		Date startTime = new Date();
		try {
			validateRequest(requestDTO);
			User user = repository.findById(requestDTO.getId())
					.orElseThrow(() -> new NoSuchElementException("Invalid user ID"));

			String filePath = user.getEditableHtml();
			if (filePath == null || filePath.isEmpty()) {
				throw new FileNotFoundException("Editable HTML file not found for user ID: " + user.getId());
			}

			String modifiedHtmlContent = modifyHtml(filePath, requestDTO.getJsonData());
			String downloadType = requestDTO.getDownloadType().trim();
			String folderPath = checkFolder("DownloadHTMLANDPDF");
			String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

			if (downloadType.equalsIgnoreCase("HTML")) {
				String newFilePath = folderPath + File.separator + user.getId() + "_editable_" + timestamp + ".html";
				writeToFile(newFilePath, modifiedHtmlContent);

				File file = new File(newFilePath);
				InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

				logService.logActivity("SUCCESS", "HTML file generated successfully",
						startTime);

				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.TEXT_HTML);
				headers.setContentDisposition(ContentDisposition.attachment().filename(file.getName()).build());
				return new ResponseEntity<>(resource, headers, HttpStatus.OK);

			} else if (downloadType.equalsIgnoreCase("PDF")) {
				String htmlFilePath = folderPath + File.separator + user.getId() + "_editable_" + timestamp + ".html";
				writeToFile(htmlFilePath, modifiedHtmlContent);

				File htmlFile = new File(htmlFilePath);
				MultipartFile multipartFile = convertFileToMultipartFile(htmlFile);

				ResponseEntity<?> response = getPdf(multipartFile, user.getId() + "_converted_" + timestamp);

				logService.logActivity("SUCCESS", "PDF generated successfully",
						startTime);
				return response;
			} else {
				String msg = "Invalid download type: " + downloadType;
				logService.logActivity( "FAILURE", msg, startTime);
				return ResponseEntity.badRequest().body(msg);
			}

		} catch (FileNotFoundException e) {
			logService.logActivity("FAILURE", e.getMessage(), startTime);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

		} catch (Exception e) {
			String msg = "Error processing file: " + e.getMessage();
			e.printStackTrace();
			logService.logActivity("FAILURE", msg, startTime);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
		}
	}

	public ResponseEntity<?> getPdf(MultipartFile multipartFile, String name) {
		try {
			String apiUrl = "https://estateagents.club/api/api/v1/s3Upload/uploadHtml";
			String fileName = multipartFile.getOriginalFilename();

			MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
			requestBody.add("name", name);

			byte[] fileData = multipartFile.getBytes();
			HttpHeaders fileHeaders = createFileHeaders(Objects.requireNonNull(fileName));
			requestBody.add("file", new HttpEntity<>(fileData, fileHeaders));

			RequestEntity<MultiValueMap<String, Object>> requestEntity = RequestEntity.post(apiUrl)
					.contentType(MediaType.MULTIPART_FORM_DATA).body(requestBody);

			ResponseEntity<Resource> response = restTemplate.exchange(requestEntity, Resource.class);

			if (response.getStatusCode() == HttpStatus.OK) {
				Resource pdfResource = response.getBody();

				String path = Mypath.getPath() + "DownloadHTMLANDPDF" + File.separator;
				Path directoryPath = Paths.get(path);

				Files.createDirectories(directoryPath);
				System.out.println("Directories created or already exist at: " + directoryPath.toString());

				String localFilePath = path + fileName.replaceFirst("[.][^.]+$", "") + ".pdf";
				File localFile = new File(localFilePath);

				try (InputStream inputStream = pdfResource.getInputStream();
						OutputStream outputStream = new FileOutputStream(localFile)) {
					byte[] buffer = new byte[1024];
					int bytesRead;
					while ((bytesRead = inputStream.read(buffer)) != -1) {
						outputStream.write(buffer, 0, bytesRead);
					}
					System.out.println("PDF downloaded and saved to: " + localFilePath);
				}

				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_PDF);
				headers.setContentDispositionFormData("attachment", "editable_html.pdf");

				Path pdfPath = localFile.toPath();

				return ResponseEntity.ok().headers(headers).body(new ByteArrayResource(Files.readAllBytes(pdfPath)));

			} else {
				return ResponseEntity.status(response.getStatusCode())
						.body(new ByteArrayResource("API request failed".getBytes()));
			}
		} catch (IOException e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ByteArrayResource("Internal server error".getBytes()));
		}
	}

	private void writeToFile(String filePath, String content) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
			writer.write(content);
			LOGGER.info("File has been written to: " + filePath);
		}
	}

	private void validateRequest(RequestDTO requestDTO) {
		if (requestDTO.getId() == null || requestDTO.getJsonData() == null || requestDTO.getDownloadType() == null) {
			throw new NullPointerException("Id, JSON data, or download type is null.");
		}
	}

	private static String modifyHtml(String filePath, String jsonData) throws IOException {

		StringBuilder htmlContent = new StringBuilder();
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		String line;
		while ((line = reader.readLine()) != null) {
			htmlContent.append(line).append("\n");
		}
		reader.close();

		String toFind = "var jsonData1";
		// int startIndex = getSecondLastIndex(htmlContent, toFind);
		// int startIndex = htmlContent.lastIndexOf("var jsonData1");

		int startIndex = htmlContent.lastIndexOf(toFind);

		if (startIndex != -1) {
			int endIndex = htmlContent.indexOf("]; var custom_language", startIndex);
			if (endIndex != -1) {
				String start = htmlContent.substring(0, startIndex + 13);
				String end = htmlContent.substring(endIndex + 2); // +2 to skip the "};"
				String finalString = start + "=[" + jsonData + "]\n" + end;
				return finalString;
			}
		}
		return htmlContent.toString();
	}

	private HttpHeaders createFileHeaders(String filename) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setContentDispositionFormData("file", filename);
		return headers;
	}

	public static MultipartFile convertFileToMultipartFile(File file) throws IOException {
		byte[] fileContent = Files.readAllBytes(file.toPath());
		MultipartFile multipartFile = new FileToMultipartFile(file.getName(), file.getName(),
				"application/octet-stream", fileContent);
		return multipartFile;
	}

	// need to check
	@PutMapping("/editTemplate/{id}")
	public ResponseEntity<String> editUser(@PathVariable Integer id, @RequestBody User user) {
		Date startTime = new Date();
		try {
			String result = serviceLogic.editUser(id, user);
			logService.logActivity("SUCCESS", result, startTime);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			String msg = "Template update failed no ID found " + id;
			logService.logActivity("FAILURE", msg, startTime);
			return ResponseEntity.badRequest().body(msg);
		}
	}

	@DeleteMapping("/deleteTemplate/{id}")
	public ResponseEntity<String> deleteUser(@PathVariable Integer id) {
		Date startTime = new Date();
		try {
			String result = serviceLogic.deleteUser(id);
			logService.logActivity( "SUCCESS", result, startTime);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			String msg = "Template deletion failed no id found: " + id;

			logService.logActivity( "FAILURE", msg, startTime);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
		}
	}

	@GetMapping("/getTemplate/{id}")
	public ResponseEntity<Map<String, Object>> getUser(@PathVariable Integer id) {
		Date startTime = new Date();
		Map<String, Object> response = new LinkedHashMap<>();

		try {
			User user = repository.findById(id)
					.orElseThrow(() -> new NoSuchElementException("Data not found with id: " + id));

			response.put("id", user.getId());
			response.put("name", user.getName());

			try {
				String htmlContent = Files.readString(Paths.get(user.getEditableHtml()));
				response.put("EditableHtml", htmlContent);
			} catch (IOException e) {
				response.put("EditableHtml", "<h1>Error loading HTML</h1>");
			}

			logService.logActivity("SUCCESS",
					"Template with id " + id + " fetched successfully for editing", startTime);
			return ResponseEntity.ok(response);

		} catch (NoSuchElementException e) {
			String msg = e.getMessage();
			logService.logActivity("FAILURE", msg, startTime);
			response.put("error", msg);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

		} catch (Exception e) {
			String msg = "Error fetching template: " + e.getMessage();
			logService.logActivity("FAILURE", msg, startTime);
			response.put("error", msg);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@GetMapping("/getTemplate")
	public ResponseEntity<?> getAllTemplates() {
		Date startTime = new Date();
		try {
			List<User> userList = repository.findAll();
			Collections.reverse(userList);

			logService.logActivity("SUCCESS", "Fetched all templates succesfully",
					startTime);
			return ResponseEntity.ok(userList);
		} catch (Exception e) {
			String msg = "Error fetching templates: " + e.getMessage();
			logService.logActivity("FAILURE", msg, startTime);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
		}
	}

	@GetMapping("/getErrorLogs")
	public ResponseEntity<?> getErrorLogs(@RequestParam(value = "startDate", required = false) String startDate,
			@RequestParam(value = "endDate", required = false) String endDate) {

		HttpHeaders headers = new HttpHeaders();
		headers.add("Access-Control-Allow-Methods", "GET");
		headers.add("Access-Control-Allow-Headers", "Content-Type");

		Date startTime = new Date();
		String typeRequested = "GET_ERROR_LOGS";

		try {
			if (ObjectUtils.isEmpty(startDate) && ObjectUtils.isEmpty(endDate)) {
				List<LogData> allLogs = logBookRepo.findAll();
				logService.logActivity("SUCCESS", "Fetched all logs", startTime);

				allLogs.sort((a, b) -> b.getSendRequestTime().compareTo(a.getSendRequestTime()));

				return ResponseEntity.ok().headers(headers).body(allLogs);
			}

			SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			Date start = null;
			Date end = null;

			if (!ObjectUtils.isEmpty(startDate)) {
				try {
					Date parsedStart = inputFormat.parse(startDate);
					String formattedStart = outputFormat.format(parsedStart);
					start = outputFormat.parse(formattedStart);
				} catch (Exception ex) {
					String msg = "Invalid startDate format. Expected format: yyyy-MM-dd";
					logService.logActivity("FAILURE", msg, startTime);
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
				}
			}

			if (!ObjectUtils.isEmpty(endDate)) {
				try {
					Date parsedEnd = inputFormat.parse(endDate);
					String formattedEnd = new SimpleDateFormat("yyyy-MM-dd").format(parsedEnd) + " 23:59:59";
					end = outputFormat.parse(formattedEnd);
				} catch (Exception ex) {
					String msg = "Invalid endDate format. Expected format: yyyy-MM-dd";
					logService.logActivity("FAILURE", msg, startTime);
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
				}
			}

			if (start != null && end != null && end.before(start)) {
				String msg = "Invalid date range: endDate cannot be before startDate.";
				logService.logActivity("FAILURE", msg, startTime);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
			}

			List<LogData> logs;
			if (start != null && end != null) {
				logs = logBookRepo.findBySendRequestTimeBetween(start, end);
			} else if (start != null) {
				logs = logBookRepo.findBySendRequestTimeAfter(start);
			} else {
				logs = logBookRepo.findBySendRequestTimeBefore(end);
			}

			logService.logActivity("SUCCESS", "Fetched logs between "
					+ (startDate != null ? startDate : "beginning") + " and " + (endDate != null ? endDate : "today"),
					startTime);

//            logs.sort((a, b) -> b.getSendRequestTime().compareTo(a.getSendRequestTime()));

			return ResponseEntity.ok().headers(headers).body(logs);

		} catch (Exception e) {
			e.printStackTrace();
			String msg = "Unexpected error fetching logs: " + e.getMessage();
			logService.logActivity("FAILURE", msg, startTime);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).headers(headers).body(msg);
		}
	}

	// N/A
//    @GetMapping("/getDataFromDates")
//    public ResponseEntity<List<LogData>> getDataFromEntry(@RequestParam("startDate") String startDate,
//                                                          @RequestParam("endDate") String endDate) throws ParseException {
//        Date startTime = new Date();
//        try {
//            if (ObjectUtils.isEmpty(startDate) || ObjectUtils.isEmpty(endDate)) {
//                List<LogData> all = logBookRepo.findAll();
//                logService.logActivity(null, "FETCH_LOGS", "SUCCESS", "Fetched all logs", startTime);
//                return ResponseEntity.ok(all);
//            }
//
//            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            Date s = new SimpleDateFormat("yyyy-MM-dd").parse(startDate);
//            Date e = new SimpleDateFormat("yyyy-MM-dd").parse(endDate);
//            e = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(new SimpleDateFormat("yyyy-MM-dd").format(e) + " 23:59:59");
//
//            List<LogData> logs = logBookRepo.findBySendRequestTimeBetween(s, e);
//            logService.logActivity(null, "FETCH_LOGS", "SUCCESS", "Fetched logs in date range", startTime);
//            return ResponseEntity.ok(logs);
//
//        } catch (Exception ex) {
//            String msg = "Error fetching logs: " + ex.getMessage();
//            logService.logActivity(null, "FETCH_LOGS", "FAILURE", msg, startTime);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
//        }
//    }
//    
	// W
//    @GetMapping("/getTemplate/{id}")
//    public ResponseEntity<User> getUser(@PathVariable Integer id) {
//        User user = repository.findById(id).orElseThrow(() -> new NoSuchElementException());
//        return ResponseEntity.ok(user);
//    }

}
