package com.example.InteractiveDesignBackend.Services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.example.InteractiveDesignBackend.Dto.RequestDTO;
import com.example.InteractiveDesignBackend.Entity.LogData;
import com.example.InteractiveDesignBackend.Entity.RecordEntity;
import com.example.InteractiveDesignBackend.Repositor.LogBookRepo;
import com.example.InteractiveDesignBackend.Repositor.RecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;


import java.net.URLDecoder;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RecordService {
	
	
	
	
	
	// private static final int PDF_RENDER_PARALLELISM = Math.max(1,
	// 		Integer.getInteger("interactive.pdf.render.parallelism", 5));

	@Value("${interactive.pdf.render.parallelism:20}")
private int pdfParallelism;

	private static final String BULK_PDF_RENDERER_URL = "http://192.168.0.188:3012/api/v1/s3Upload/uploadHTML6";

	private static class PdfGenerationResult {

		private final String sourceName;
		private final String fileType;
		private final byte[] pdfBytes;
		private final String errorMessage;

		private PdfGenerationResult(
				String sourceName,
				String fileType,
				byte[] pdfBytes,
				String errorMessage) {

			this.sourceName = sourceName;
			this.fileType = fileType;
			this.pdfBytes = pdfBytes;
			this.errorMessage = errorMessage;
		}

		private static PdfGenerationResult success(
				String sourceName,
				String fileType,
				byte[] pdfBytes) {

			return new PdfGenerationResult(sourceName, fileType, pdfBytes, null);
		}

		private static PdfGenerationResult failure(
				String sourceName,
				String errorMessage) {

			return new PdfGenerationResult(sourceName, null, null, errorMessage);
		}

		private boolean isSuccess() {
			return pdfBytes != null && pdfBytes.length > 0;
		}
	}

	

	@Autowired
	private RecordRepository repository;

	@Autowired
	private LogBookRepo logRepository;

	@Autowired
	private LogService logService;

	@Autowired
@Qualifier("pdfRestTemplate")
private RestTemplate pdfRestTemplate; 
	
	public byte[] SingleHtmlToPdf(MultipartFile htmlFile) throws IOException {

	    // RestTemplate restTemplate = new RestTemplate();

	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

	    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

	    body.add("file", new ByteArrayResource(htmlFile.getBytes()) {
	        @Override
	        public String getFilename() {
	            return htmlFile.getOriginalFilename();
	        }
	    });

	    HttpEntity<MultiValueMap<String, Object>> requestEntity =
	            new HttpEntity<>(body, headers);

	    String apiUrl =
	            "http://192.168.0.188:3012/api/v1/s3Upload/uploadHtmlSinglePage";

//	    String apiUrl =
//	    "http://api.ariantechsolutions.in/interactive-server/api/v1/s3Upload/uploadHtmlSinglePage";

	    try {

	        // ResponseEntity<byte[]> response = restTemplate.exchange(
	        //         apiUrl,
	        //         HttpMethod.POST,
	        //         requestEntity,
	        //         byte[].class
	        // );

			ResponseEntity<byte[]> response = pdfRestTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                requestEntity,
                byte[].class
        );

	        if (response.getBody() == null || response.getBody().length == 0) {

	            logService.logActivity(
	                    "FAILURE",
	                    "Error while calling remote API",
	                    new Date()
	            );

	            throw new IOException("Error API returned empty response");
	        }

	        return response.getBody();

	    } catch (Exception ex) {

	        logService.logActivity(
	                "ERROR",
	                "Error while calling remote API: " + ex.getMessage(),
	                new Date()
	        );

	        throw new IOException("Error API call failed: " + ex.getMessage());
	    }
	}

	public void processZipAndGeneratePdfFast(
			String payloadJson,
			MultipartFile zipFile,
			MultipartFile htmlFile,
			OutputStream responseStream) throws Exception {

		ObjectMapper mapper = new ObjectMapper();

		JsonNode payloadNode = mapper.readTree(payloadJson);

		JsonNode mappingNode;

		String pageSize = "A4";

		String orientation = "portrait";

		if (payloadNode.isArray()) {

			mappingNode = payloadNode;

		} else if (payloadNode.has("mapping")) {

			mappingNode = payloadNode.get("mapping");

			if (payloadNode.has("pageSize")) {
				pageSize = payloadNode.get("pageSize").asText();
			}

			if (payloadNode.has("orientation")) {
				orientation = payloadNode.get("orientation").asText();
			}

		} else {

			throw new IllegalArgumentException("Invalid payload format.");
		}

		Map<String, JsonNode> htmlIdToJsonField = new LinkedHashMap<>();

		for (JsonNode obj : mappingNode) {

			obj.fields().forEachRemaining(entry -> htmlIdToJsonField.put(
					entry.getKey(),
					entry.getValue()));
		}

		List<String> fileNameFields = extractTextList(htmlIdToJsonField.get("file_name"));

		List<String> passwordFields = extractTextList(htmlIdToJsonField.get("password"));

		// String htmlContent = new String(
		// 		htmlFile.getBytes(),
		// 		StandardCharsets.UTF_8).replaceFirst("^\uFEFF", "");

		String htmlContent = new String(
        htmlFile.getBytes(),
        StandardCharsets.UTF_8).replaceFirst("^\uFEFF", "");

// Parse once only
final Document masterDoc = Jsoup.parse(htmlContent);

		// RestTemplate restTemplate = new RestTemplate();

		Set<String> usedNames = new HashSet<>();

		StringBuilder generationErrors = new StringBuilder();

		List<RecordEntity> records =
        Collections.synchronizedList(
                new ArrayList<>());

		ExecutorService executor = Executors.newFixedThreadPool(pdfParallelism);

		CompletionService<PdfGenerationResult> completionService = new ExecutorCompletionService<>(executor);

		int submitted = 0;

		int completed = 0;

		int generatedCount = 0;

		try (
				ZipInputStream zis = new ZipInputStream(zipFile.getInputStream());
				ZipOutputStream zos = new ZipOutputStream(responseStream)) {

			zos.setLevel(java.util.zip.Deflater.BEST_SPEED); // ADD THIS
			ZipEntry jsonEntry;

			while ((jsonEntry = zis.getNextEntry()) != null) {

				final String entryName = jsonEntry.getName();

				try {

					if (jsonEntry.isDirectory()
							|| !entryName.toLowerCase().endsWith(".json")) {
						continue;
					}

					byte[] jsonBytes = readCurrentZipEntry(zis);

					if (jsonBytes.length == 0) {
						continue;
					}

					completionService.submit(
							createBulkPdfTask(
									entryName,
									jsonBytes,
									mapper,
									pdfRestTemplate,
									masterDoc,
									htmlIdToJsonField,
									fileNameFields,
									passwordFields,
									pageSize,
									orientation));

					// submitted++;

					// if (submitted - completed >= pdfParallelism) {

					// 	PdfGenerationResult result = takePdfResult(completionService);

					// 	completed++;

					// 	if (writePdfResultToZip(
					// 			result,
					// 			zos,
					// 			usedNames,
					// 			generationErrors,
					// 		records)) {
					// 		generatedCount++;
					// 	}
					// }

					submitted++;

// CHANGED: drain ALL ready results, not just one — keeps pipeline full
while (submitted - completed >= pdfParallelism) {
    System.out.println("Submitted=" + submitted + " Completed=" + completed);
    PdfGenerationResult result = takePdfResult(completionService);
    completed++;
    if (writePdfResultToZip(result, zos, usedNames, generationErrors, records)) {
        generatedCount++;
    }
}

				} catch (Exception ex) {

					generationErrors
							.append(entryName)
							.append(" -> ")
							.append(ex.getMessage())
							.append(System.lineSeparator());

				} finally {

					zis.closeEntry();
				}
			}

			while (completed < submitted) {

				PdfGenerationResult result = takePdfResult(completionService);

				completed++;

				if (writePdfResultToZip(
						result,
						zos,
						usedNames,
						generationErrors,
					records)) {
					generatedCount++;
				}
			}

			if (generatedCount == 0) {

				if (generationErrors.length() == 0) {
					generationErrors.append(
							"No PDF generated. Check that the ZIP contains valid .json files "
									+ "and that the payload mapping matches the JSON field names.");
				}

				zos.putNextEntry(new ZipEntry("_generation_errors.txt"));

				zos.write(
						generationErrors.toString().getBytes(StandardCharsets.UTF_8));

				zos.closeEntry();
			}

			zos.finish();

			zos.flush();

		} finally {
    executor.shutdown();
    try {
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
    } catch (InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
    }
    // ADDED: batch DB insert instead of per-record saves
    if (!records.isEmpty()) {
        try {
            repository.saveAll(records);
        } catch (Exception e) {
            System.err.println("Batch save failed: " + e.getMessage());
        }
    }
}
	}

	private byte[] readCurrentZipEntry(ZipInputStream zis) throws IOException {
		ByteArrayOutputStream jsonBaos = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int len;
		while ((len = zis.read(buffer)) != -1) {
			jsonBaos.write(buffer, 0, len);
		}
		return jsonBaos.toByteArray();
	}

	private Callable<PdfGenerationResult> createBulkPdfTask(
			String sourceName,
			byte[] jsonBytes,
			ObjectMapper mapper,
			RestTemplate restTemplate,
			  Document masterDoc,
			Map<String, JsonNode> htmlIdToJsonField,
			List<String> fileNameFields,
			List<String> passwordFields,
			String pageSize,
			String orientation) {

		return () -> {

			try {

				JsonNode dataJson = mapper.readTree(jsonBytes);

				Iterator<Map.Entry<String, JsonNode>> users = dataJson.fields();

				if (!users.hasNext()) {
					return PdfGenerationResult.failure(
							sourceName,
							"JSON file has no root object");
				}

				Map.Entry<String, JsonNode> entry = users.next();

				String userKey = entry.getKey();

				JsonNode userNode = entry.getValue();

				Map<String, JsonNode> normalizedFieldMap = normalizeUserFields(userKey, userNode, dataJson);

				// Document doc = Jsoup.parse(htmlContent);
				Document doc = masterDoc.clone();

				fillTemplate(doc, htmlIdToJsonField, normalizedFieldMap, userKey);

				String fileType = buildFirstFileName(
						fileNameFields,
						normalizedFieldMap,
						userKey);

				byte[] pdfBytes = renderPdf(
						restTemplate,
						mapper,
						doc.outerHtml(),
						dataJson,
						fileType,
						pageSize,
						orientation,
						BULK_PDF_RENDERER_URL);

				pdfBytes = protectPdfLikeUploadPdf2(
						pdfBytes,
						passwordFields,
						normalizedFieldMap,
						userKey);

				return PdfGenerationResult.success(sourceName, fileType, pdfBytes);

			} catch (Exception ex) {

				return PdfGenerationResult.failure(
						sourceName,
						ex.getMessage() == null ? ex.toString() : ex.getMessage());
			}
		};
	}

	private String buildFirstFileName(
			List<String> fileNameFields,
			Map<String, JsonNode> normalizedFieldMap,
			String userKey) {

		String fileType = "file_" + UUID.randomUUID();

		if (fileNameFields.isEmpty()) {
			return fileType;
		}

		for (String fnExpr : fileNameFields) {

			String fnValue = resolveFieldValueWithIndexes(
					normalizedFieldMap,
					userKey + "." + fnExpr.trim());

			if (fnValue != null && !fnValue.isEmpty()) {
				return fnValue;
			}
		}

		return fileType;
	}

	private byte[] protectPdfLikeUploadPdf2(
			byte[] pdfBytes,
			List<String> passwordFields,
			Map<String, JsonNode> normalizedFieldMap,
			String userKey) throws IOException {



    if (passwordFields == null || passwordFields.isEmpty()) {  // CHANGE: add null check
        return pdfBytes;
    }

		// if (passwordFields.isEmpty()) {
		// 	return pdfBytes;
		// }

		StringBuilder pwBuilder = new StringBuilder();

		for (String pwExpr : passwordFields) {

			String pwValue;

			if (pwExpr.contains(".")) {

				pwValue = resolveFieldValueWithIndexes(
						normalizedFieldMap,
						userKey + "." + pwExpr.trim());

			} else {

				pwValue = pwExpr.trim();
			}

			if (pwValue != null) {
				pwBuilder.append(pwValue.trim());
			}
		}

		String userPassword = pwBuilder.toString().trim();

		if (userPassword.isEmpty()) {
			return pdfBytes;
		}

		try (
				PDDocument document = PDDocument.load(pdfBytes);
				ByteArrayOutputStream securedOut = new ByteArrayOutputStream()) {

			AccessPermission permissions = new AccessPermission();

			StandardProtectionPolicy policy = new StandardProtectionPolicy(
					userPassword,
					userPassword,
					permissions);

			policy.setEncryptionKeyLength(128);

			policy.setPermissions(permissions);

			document.protect(policy);

			document.save(securedOut);

			return securedOut.toByteArray();
		}
	}

	private PdfGenerationResult takePdfResult(
	        CompletionService<PdfGenerationResult> completionService)
	        throws IOException {

	    try {

	        Future<PdfGenerationResult> future =
	                completionService.poll(10, TimeUnit.MINUTES);

	        if (future == null) {

	            throw new IOException(
	                    "Timeout waiting for PDF generation");
	        }

	        return future.get();

	    } catch (InterruptedException ex) {

	        Thread.currentThread().interrupt();

	        throw new IOException(
	                "PDF ZIP generation interrupted",
	                ex);

	    } catch (ExecutionException ex) {

	        throw new IOException(
	                "PDF generation task failed",
	                ex.getCause());
	    }
	}

	private boolean writePdfResultToZip(
        PdfGenerationResult result,
        ZipOutputStream zos,
        Set<String> usedNames,
        StringBuilder generationErrors,
        List<RecordEntity> records) throws IOException {

		if (result == null || !result.isSuccess()) {

			generationErrors
					.append(result != null ? result.sourceName : "unknown")
					.append(" -> ")
					.append(result != null ? result.errorMessage : "Unknown PDF generation error")
					.append(System.lineSeparator());

			return false;
		}

		String zipEntryName = uniqueZipEntryName(
				sanitizeFileName(result.fileType),
				"pdf",
				usedNames);

		zos.putNextEntry(new ZipEntry(zipEntryName));

		zos.write(result.pdfBytes);

		zos.closeEntry();

		records.add(
        RecordEntity.builder()
                .fileName(zipEntryName)
                .build());

		return true;
	}

	private String sanitizeFileName(String fileName) {
		String sanitized = fileName == null ? ""
				: fileName.trim()
						.replaceAll("[\\\\/:*?\"<>|]", "_")
						.replaceAll("\\s+", "_");
		if (sanitized.isEmpty())
			return "file_" + UUID.randomUUID();
		return sanitized;
	}

	private String uniqueZipEntryName(String baseName, String extension, Set<String> usedNames) {
		String name = baseName + "." + extension;
		int counter = 2;
		while (usedNames.contains(name.toLowerCase())) {
			name = baseName + "_" + counter + "." + extension;
			counter++;
		}
		usedNames.add(name.toLowerCase());
		return name;
	}

	private String resolveFieldValueWithIndexes(
			Map<String, JsonNode> normalizedFieldMap,
			String expression) {

		if (expression == null
				|| expression.trim().isEmpty()) {
			return null;
		}

		try {

			expression = expression.trim();

			String[] parts = expression.split("\\.");

			JsonNode currentNode = null;

			// root node
			if (normalizedFieldMap.containsKey(parts[0].toLowerCase())) {

				currentNode = normalizedFieldMap.get(
						parts[0].toLowerCase());
			}

			// navigate
			for (int i = 1; i < parts.length && currentNode != null; i++) {

				String part = parts[i];

				// array/index handling
				if (part.contains("[")
						&& part.contains("]")) {

					String field = part.substring(
							0,
							part.indexOf("["));

					JsonNode arrayNode = currentNode.get(field);

					if (arrayNode == null) {
						return null;
					}

					String indexPart = part.substring(
							part.indexOf("[") + 1,
							part.indexOf("]"));

					String[] indexes = indexPart.split(",");

					// text slicing
					if (arrayNode.isValueNode()) {

						String value = arrayNode.asText();

						value = value.replaceAll(
								"[-_/\\s]",
								"");

						StringBuilder sb = new StringBuilder();

						for (String idxStr : indexes) {

							try {

								int idx = Integer.parseInt(
										idxStr.trim());

								if (idx >= 0
										&& idx < value.length()) {

									sb.append(
											value.charAt(idx));
								}

							} catch (Exception ignore) {
							}
						}

						return sb.toString();
					}

					// json array
					else if (arrayNode.isArray()) {

						int idx = Integer.parseInt(
								indexes[0].trim());

						if (idx >= 0
								&& idx < arrayNode.size()) {

							currentNode = arrayNode.get(idx);

						} else {

							return null;
						}
					}

				} else {

					currentNode = currentNode.get(part);
				}
			}

			// final value
			if (currentNode != null
					&& currentNode.isValueNode()) {

				return currentNode.asText();
			}

			return null;

		} catch (Exception e) {

			System.out.println(
					"Resolver error : "
							+ expression
							+ " -> "
							+ e.getMessage());

			return null;
		}
	}

	private String resolveDesignerTemplateValue(
			Element elem,
			Map<String, JsonNode> normalizedFieldMap,
			String userKey) {

		String templateText = elem.attr("data-template-text");

		if (templateText == null || templateText.trim().isEmpty()) {
			templateText = elem.html();
		} else {
			try {
				templateText = URLDecoder.decode(
						templateText,
						StandardCharsets.UTF_8.name());
			} catch (Exception ignore) {
			}
		}

		return renderTemplateText(
				templateText,
				elem,
				normalizedFieldMap,
				userKey);
	}

	private Element findTemplateElementForDesignerField(
			Document doc,
			String payloadId,
			String fieldRef,
			Map<String, JsonNode> normalizedFieldMap,
			String userKey) {

		if (doc == null) {
			return null;
		}

		for (Element candidate : doc.select("[my-input-json]")) {
			String path = candidate.attr("my-input-json").trim();

			if (path.equals(fieldRef)
					|| path.equals("__i_designer_template_values." + payloadId)) {
				return candidate;
			}
		}

		String helperValue = resolveFieldValueWithIndexes(normalizedFieldMap, fieldRef);

		for (Element candidate : doc.select("[data-template-text], [my-input-json]")) {
			String templateText = candidate.attr("data-template-text");

			if (templateText == null || templateText.trim().isEmpty()) {
				templateText = candidate.html();
			} else {
				templateText = normalizeTemplateText(templateText);
			}

			if (templateText == null || !templateText.contains("{")) {
				continue;
			}

			if (helperValue != null && helperValue.contains("{")
					&& normalizeTemplateText(templateText).equals(normalizeTemplateText(helperValue))) {
				return candidate;
			}

			String renderedValue = renderTemplateText(
					templateText,
					candidate,
					normalizedFieldMap,
					userKey);

			if (renderedValue != null
					&& !renderedValue.trim().isEmpty()
					&& !renderedValue.contains("{")) {
				return candidate;
			}
		}

		return null;
	}

	private String normalizeTemplateText(String value) {
		if (value == null) {
			return "";
		}

		try {
			value = URLDecoder.decode(value, StandardCharsets.UTF_8.name());
		} catch (Exception ignore) {
		}

		return value.replaceAll("\\s+", " ").trim();
	}


	private String renderTemplateText(
			String templateText,
			Element elem,
			Map<String, JsonNode> normalizedFieldMap,
			String userKey) {

		Matcher matcher = Pattern.compile("\\{([^{}]+)}")
				.matcher(templateText == null ? "" : templateText);

		StringBuffer rendered = new StringBuffer();

		while (matcher.find()) {

			String token = matcher.group(1).trim();

			String value = resolveTemplateTokenValue(
					normalizedFieldMap,
					userKey,
					token);

			if (value == null || value.isEmpty()) {
				value = resolveTemplateDatasourceValue(
						elem,
						normalizedFieldMap,
						userKey);
			}

			matcher.appendReplacement(
					rendered,
					Matcher.quoteReplacement(value == null ? "" : value));
		}

		matcher.appendTail(rendered);

		return rendered.toString();
	}

	private String resolveTemplateDatasourceValue(
			Element elem,
			Map<String, JsonNode> normalizedFieldMap,
			String userKey) {

		if (elem == null) {
			return null;
		}

	String[] pathAttributes = {
			"my-input-json",
			"data-json-path",
			"data-datasource-path",
			"jsonpath"
	};

	for (String attribute : pathAttributes) {

			String path = elem.attr(attribute);

			if (path == null || path.trim().isEmpty()) {
				continue;
			}

			path = path.trim();

			String value = resolveFieldValueWithIndexes(
					normalizedFieldMap,
					path);

			if (value == null || value.isEmpty()) {
				value = resolveFieldValueWithIndexes(
						normalizedFieldMap,
						path.startsWith(userKey + ".")
								? path
								: userKey + "." + path);
			}

			if (value != null && !value.isEmpty()) {
			return value;
		}
	}

	String cssPath = resolveCssDatasourcePath(elem);

	if (cssPath != null && !cssPath.trim().isEmpty()) {
		cssPath = cssPath.trim();

		String value = resolveFieldValueWithIndexes(
				normalizedFieldMap,
				cssPath);

		if (value == null || value.isEmpty()) {
			value = resolveFieldValueWithIndexes(
					normalizedFieldMap,
					cssPath.startsWith(userKey + ".")
							? cssPath
							: userKey + "." + cssPath);
		}

		if (value != null && !value.isEmpty()) {
			return value;
		}
	}

	return null;
}

    private String resolveCssDatasourcePath(Element elem) {

	String id = elem.id();

	if (id == null || id.trim().isEmpty()
			|| elem.ownerDocument() == null) {
		return null;
	}

	Pattern blockPattern = Pattern.compile(
			"#" + Pattern.quote(id.trim()) + "\\s*\\{([^}]*)}",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	Pattern pathPattern = Pattern.compile(
			"(?:^|;)\\s*my-input-json\\s*:\\s*([^;]+)",
			Pattern.CASE_INSENSITIVE);

	for (Element style : elem.ownerDocument().select("style")) {
		Matcher blockMatcher = blockPattern.matcher(style.data());

		while (blockMatcher.find()) {
			Matcher pathMatcher = pathPattern.matcher(blockMatcher.group(1));

			if (pathMatcher.find()) {
				return pathMatcher.group(1).trim();
			}
		}
	}

	return null;
}

	private String resolveTemplateTokenValue(
			Map<String, JsonNode> normalizedFieldMap,
			String userKey,
			String token) {

		if (token == null || token.trim().isEmpty()) {
			return null;
		}

		String expression = token.trim();

		String value = resolveFieldValueWithIndexes(
				normalizedFieldMap,
				expression.startsWith(userKey + ".")
						? expression
						: userKey + "." + expression);

		if (value == null || value.isEmpty()) {
			value = resolveFieldValueWithIndexes(
					normalizedFieldMap,
					expression);
		}

	if (value == null || value.isEmpty()) {
		value = resolveFieldValueWithIndexes(
				normalizedFieldMap,
				expression.toLowerCase());
	}

	if (value == null || value.isEmpty()) {
		value = resolveJsonFieldByName(
				normalizedFieldMap.get("__root__"),
				expression);
	}

	return value;
}

    private String resolveJsonFieldByName(JsonNode node, String fieldName) {

	if (node == null || fieldName == null || fieldName.trim().isEmpty()) {
		return null;
	}

	String target = fieldName.trim();

	if (node.isObject()) {
		Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> field = fields.next();

			if (field.getKey().equalsIgnoreCase(target)
					&& field.getValue() != null
					&& field.getValue().isValueNode()) {
				return field.getValue().asText();
			}

			String nestedValue = resolveJsonFieldByName(field.getValue(), target);

			if (nestedValue != null && !nestedValue.isEmpty()) {
				return nestedValue;
			}
		}
	}

	if (node.isArray()) {
		for (JsonNode item : node) {
			String nestedValue = resolveJsonFieldByName(item, target);

			if (nestedValue != null && !nestedValue.isEmpty()) {
				return nestedValue;
			}
		}
	}

	return null;
}



    private List<String> extractTextList(JsonNode node) {
	List<String> values = new ArrayList<>();
	if (node == null)
		return values;

	if (node.isTextual()) {
		for (String value : node.asText().split(",")) {
			if (!value.trim().isEmpty())
				values.add(value.trim());
		}
	} else if (node.isArray()) {
		node.forEach(n -> {
			if (!n.asText().trim().isEmpty())
				values.add(n.asText().trim());
		});
	}
	return values;
}

    private Map<String, JsonNode> normalizeUserFields(
		String userKey,
		JsonNode userNode) {

	Map<String, JsonNode> normalizedFieldMap = new HashMap<>();

	// root object
	normalizedFieldMap.put(
			userKey.toLowerCase(),
			userNode);

	// direct + prefixed fields
	userNode.fieldNames().forEachRemaining(field -> {

		JsonNode value = userNode.get(field);

		// direct field
		normalizedFieldMap.put(
				field.toLowerCase(),
				value);

		// prefixed field
		normalizedFieldMap.put(
				userKey.toLowerCase()
						+ "."
						+ field.toLowerCase(),
				value);
	});

	return normalizedFieldMap;
}

    private Map<String, JsonNode> normalizeUserFields(
		String userKey,
		JsonNode userNode,
		JsonNode rootNode) {

	Map<String, JsonNode> normalizedFieldMap = normalizeUserFields(userKey, userNode);

	if (rootNode != null && rootNode.isObject()) {
		normalizedFieldMap.put("__root__", rootNode);
		rootNode.fieldNames().forEachRemaining(field -> normalizedFieldMap.put(
				field.toLowerCase(),
				rootNode.get(field)));
	}

	return normalizedFieldMap;
}

	private void fillTemplate(
			Document doc,
			Map<String, JsonNode> htmlIdToJsonField,
			Map<String, JsonNode> normalizedFieldMap,
			String userKey) {

		htmlIdToJsonField.forEach((id, nodeRef) -> {

			try {

				String fieldRef = nodeRef.isTextual()
						? nodeRef.asText().trim()
						: null;

				if (fieldRef == null
						|| fieldRef.isEmpty()) {
					return;
				}

				boolean designerTemplateField = fieldRef.startsWith("__i_designer_template_values");

				Element elem = doc.getElementById(id);

				if (elem == null && designerTemplateField) {
					elem = findTemplateElementForDesignerField(
							doc,
							id,
							fieldRef,
							normalizedFieldMap,
							userKey);
				}

				if (elem == null) {
					return;
				}

				String value;

				if (designerTemplateField) {

					value = resolveFieldValueWithIndexes(
							normalizedFieldMap,
							fieldRef);

					if (value == null || value.isEmpty()) {
						value = resolveFieldValueWithIndexes(
								normalizedFieldMap,
								userKey + "." + fieldRef);
					}

					if (value == null || value.isEmpty()) {
						value = resolveDesignerTemplateValue(
								elem,
								normalizedFieldMap,
								userKey);
					}

					if (value != null && value.contains("{")) {
						value = renderTemplateText(
								value,
								elem,
								normalizedFieldMap,
								userKey);
					}

				} else {

					String fullPath = fieldRef.startsWith(userKey + ".")
							? fieldRef
							: userKey + "." + fieldRef;

					value = resolveFieldValueWithIndexes(
							normalizedFieldMap,
							fullPath);

					if (value == null || value.isEmpty()) {

						value = resolveFieldValueWithIndexes(
								normalizedFieldMap,
								fullPath.toLowerCase());
					}
				}

				if (value != null
						&& !value.trim().isEmpty()) {

					if (designerTemplateField) {

						elem.html(value.trim());
						elem.removeAttr("my-input-json");
						elem.removeAttr("data-template-text");

						return;
					}

					String existingHtml = elem.html();

					// ONLY replace matching placeholder
					String placeholder = "{" + fieldRef + "}";

					if (existingHtml != null
							&& existingHtml.contains(placeholder)) {

						existingHtml = existingHtml.replace(
								placeholder,
								value.trim());

						elem.html(existingHtml);

					} else {

						// fallback normal replace
						elem.text(value.trim());
					}
				}

			} catch (Exception ex) {

				System.out.println(
						"Template mapping error : "
								+ id
								+ " -> "
								+ ex.getMessage());
			}
		});
	}


    private String buildFileName(
		List<String> fileNameFields,
		Map<String, JsonNode> normalizedFieldMap,
		String userKey) {

	if (fileNameFields.isEmpty())
		return "file_" + userKey + "_" + UUID.randomUUID();

	StringBuilder builder = new StringBuilder();
	for (String fnExpr : fileNameFields) {
		String fnValue = resolveFieldValueWithIndexes(
				normalizedFieldMap,
				userKey + "." + fnExpr.trim());
		if (fnValue != null && !fnValue.isEmpty()) {
			builder.append(fnValue);
		}
	}

	if (builder.length() == 0)
		return "file_" + userKey + "_" + UUID.randomUUID();

	return builder.toString();
}



    private byte[] renderPdf(
		RestTemplate restTemplate,
		ObjectMapper mapper,
		String html,
		JsonNode dataJson,
		String fileType,
		String pageSize,
		String orientation,
		String apiUrl) throws IOException {

	try {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("file", new ByteArrayResource(html.getBytes(StandardCharsets.UTF_8)) {
			@Override
			public String getFilename() {
				return "template.html";
			}
		});
		body.add("name", fileType);
		body.add("chartData", mapper.writeValueAsString(dataJson));

		Map<String, Object> pdfConfig = new HashMap<>();
		pdfConfig.put("pageSize", pageSize);
		pdfConfig.put("orientation", orientation);
		body.add("payload", mapper.writeValueAsString(pdfConfig));

		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

		ResponseEntity<byte[]> response = restTemplate.exchange(
				apiUrl,
				HttpMethod.POST,
				requestEntity,
				byte[].class);

		if (response.getBody() == null || response.getBody().length == 0)
			throw new IOException("Empty PDF response for " + fileType);

		return response.getBody();
	} catch (Exception ex) {
		throw new IOException("PDF generation failed for " + fileType + ": " + ex.getMessage(), ex);
	}
}



//private byte[] renderPdf(
//		RestTemplate restTemplate,
//		ObjectMapper mapper,
//		String html,
//		JsonNode dataJson,
//		String fileType,
//		String pageSize,
//		String orientation) throws IOException {
//
//	return renderPdf(
//			restTemplate,
//			mapper,
//			html,
//			dataJson,
//			fileType,
//			pageSize,
//			orientation,
//			"http://localhost:3012/api/v1/s3Upload/uploadHTML6");
//}
//
//private byte[] protectPdfIfNeeded(
//		byte[] pdfBytes,
//		List<String> passwordFields,
//		Map<String, JsonNode> normalizedFieldMap,
//		String userKey) throws IOException {
//
//	if (passwordFields.isEmpty())
//		return pdfBytes;
//
//	StringBuilder pwBuilder = new StringBuilder();
//	for (String pwExpr : passwordFields) {
//		String pwValue;
//		if (pwExpr.contains(".")) {
//			pwValue = resolveFieldValueWithIndexes(
//					normalizedFieldMap,
//					userKey + "." + pwExpr.trim());
//		} else {
//			pwValue = resolveFieldValueWithIndexes(
//					normalizedFieldMap,
//					userKey + "." + pwExpr.trim());
//			if (pwValue == null)
//				pwValue = pwExpr.trim();
//		}
//
//		if (pwValue != null)
//			pwBuilder.append(pwValue.trim());
//	}
//
//	String userPassword = pwBuilder.toString().trim();
//	if (userPassword.isEmpty())
//		return pdfBytes;
//
//	try (PDDocument document = PDDocument.load(pdfBytes);
//			ByteArrayOutputStream securedOut = new ByteArrayOutputStream()) {
//
//		AccessPermission permissions = new AccessPermission();
//		StandardProtectionPolicy policy = new StandardProtectionPolicy(userPassword, userPassword, permissions);
//		policy.setEncryptionKeyLength(128);
//		policy.setPermissions(permissions);
//		document.protect(policy);
//		document.save(securedOut);
//		return securedOut.toByteArray();
//	}
//}

	
	
	public byte[] processAndGeneratePdf(
	        MultipartFile htmlFile,
	        String payload) throws IOException {

	    RestTemplate restTemplate = new RestTemplate();

	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

	    MultiValueMap<String, Object> body =
	            new LinkedMultiValueMap<>();

	    body.add("file", new ByteArrayResource(htmlFile.getBytes()) {

	        @Override
	        public String getFilename() {
	            return htmlFile.getOriginalFilename();
	        }
	    });

	    body.add("payload", payload);

	    HttpEntity<MultiValueMap<String, Object>> requestEntity =
	            new HttpEntity<>(body, headers);

	    String apiUrl =
	            "http://localhost:3012/api/v1/s3Upload/uploadNoramlPdf";

	    try {

	        ResponseEntity<byte[]> response =
	                restTemplate.exchange(
	                        apiUrl,
	                        HttpMethod.POST,
	                        requestEntity,
	                        byte[].class
	                );

	        if (response.getBody() == null ||
	                response.getBody().length == 0) {

	            logService.logActivity(
	                    "FAILURE",
	                    "Empty response from Node",
	                    new Date()
	            );

	            throw new IOException("Node returned empty PDF");
	        }

	        return response.getBody();

	    } catch (Exception ex) {

	        logService.logActivity(
	                "ERROR",
	                "Node API error: " + ex.getMessage(),
	                new Date()
	        );

	        throw new IOException(
	                "Remote API failed: " + ex.getMessage()
	        );
	    }
	}

	
	public Map<String, byte[]> processAndGeneratePdf(
	        String payloadJson,
	        MultipartFile[] files,
	        MultipartFile htmlFile) throws IOException {

	    Date startTime = new Date();

	    ObjectMapper mapper = new ObjectMapper();

	    JsonNode payloadNode = mapper.readTree(payloadJson);

	    JsonNode mappingNode;

	    String pageSize = "A4";

	    String orientation = "portrait";

	    if (payloadNode.isArray()) {

	        mappingNode = payloadNode;

	    } else if (payloadNode.has("mapping")) {

	        mappingNode = payloadNode.get("mapping");

	        if (payloadNode.has("pageSize"))
	            pageSize = payloadNode.get("pageSize").asText();

	        if (payloadNode.has("orientation"))
	            orientation = payloadNode.get("orientation").asText();

	    } else {

	        throw new IllegalArgumentException("Invalid payload format.");
	    }

	    Map<String, JsonNode> htmlIdToJsonField =
	            new LinkedHashMap<>();

	    for (JsonNode obj : mappingNode) {

	        obj.fields().forEachRemaining(entry ->
	                htmlIdToJsonField.put(
	                        entry.getKey(),
	                        entry.getValue()
	                ));
	    }

	    // filename fields
	    List<String> fileNameFields = new ArrayList<>();

	    JsonNode fileNameNode =
	            htmlIdToJsonField.get("file_name");

	    if (fileNameNode != null) {

	        if (fileNameNode.isTextual()) {

	            fileNameFields.addAll(
	                    Arrays.asList(fileNameNode.asText().split(","))
	            );

	        } else if (fileNameNode.isArray()) {

	            fileNameNode.forEach(
	                    n -> fileNameFields.add(n.asText())
	            );
	        }
	    }

	    // password fields
	    List<String> passwordFields = new ArrayList<>();

	    JsonNode passwordNode =
	            htmlIdToJsonField.get("password");

	    if (passwordNode != null) {

	        if (passwordNode.isTextual()) {

	            passwordFields.addAll(
	                    Arrays.asList(passwordNode.asText().split(","))
	            );

	        } else if (passwordNode.isArray()) {

	            passwordNode.forEach(
	                    n -> passwordFields.add(n.asText())
	            );
	        }
	    }

	    String htmlContent =
	            new String(
	                    htmlFile.getBytes(),
	                    StandardCharsets.UTF_8
	            ).replaceFirst("^\uFEFF", "");

	    // UPDATED
	    Map<String, byte[]> pdfMap = new LinkedHashMap<>();

	    RestTemplate restTemplate = new RestTemplate();

	    for (MultipartFile file : files) {

	        JsonNode dataJson =
	                mapper.readTree(file.getInputStream());

	        Iterator<Map.Entry<String, JsonNode>> users =
	                dataJson.fields();

	        if (!users.hasNext())
	            continue;

	        Map.Entry<String, JsonNode> entry =
	                users.next();

	        String userKey = entry.getKey();

	        JsonNode userNode = entry.getValue();

	        Map<String, JsonNode> normalizedFieldMap =
	                new HashMap<>();

	        normalizedFieldMap.put(
	                userKey.toLowerCase(),
	                userNode
	        );

	        userNode.fieldNames().forEachRemaining(field ->
	                normalizedFieldMap.put(
	                        field.toLowerCase(),
	                        userNode.get(field)
	                ));

	        Document doc = Jsoup.parse(htmlContent);

	        AtomicBoolean hasValidData =
	                new AtomicBoolean(false);

	        htmlIdToJsonField.forEach((id, nodeRef) -> {

	            String fieldRef =
	                    nodeRef.isTextual()
	                            ? nodeRef.asText()
	                            : null;

	            Element elem = doc.getElementById(id);

	            if (elem == null)
	                return;

	            if (fieldRef != null) {

	                String fullPath =
	                        fieldRef.startsWith(userKey + ".")
	                                ? fieldRef
	                                : userKey + "." + fieldRef;

	                String value =
	                        resolveFieldValueWithIndexes(
	                                normalizedFieldMap,
	                                fullPath
	                        );

	                if (value != null && !value.isEmpty()) {

	                    elem.text(value);

	                    hasValidData.set(true);
	                }
	            }
	        });

	        String fileType =
	                "file_" + UUID.randomUUID();

	        if (!fileNameFields.isEmpty()) {

	            for (String fnExpr : fileNameFields) {

	                String fullPath =
	                        userKey + "." + fnExpr.trim();

	                String fnValue =
	                        resolveFieldValueWithIndexes(
	                                normalizedFieldMap,
	                                fullPath
	                        );

	                if (fnValue != null && !fnValue.isEmpty()) {

	                    fileType = fnValue;

	                    break;
	                }
	            }
	        }

	        try {

	            HttpHeaders headers = new HttpHeaders();

	            headers.setContentType(
	                    MediaType.MULTIPART_FORM_DATA
	            );

	            MultiValueMap<String, Object> body =
	                    new LinkedMultiValueMap<>();

	            body.add(
	                    "file",
	                    new ByteArrayResource(
	                            doc.outerHtml()
	                                    .getBytes(StandardCharsets.UTF_8)
	                    ) {

	                        @Override
	                        public String getFilename() {

	                            return "template.html";
	                        }
	                    }
	            );

	            body.add("name", fileType);

	            body.add(
	                    "chartData",
	                    mapper.writeValueAsString(dataJson)
	            );

	            Map<String, Object> pdfConfig =
	                    new HashMap<>();

	            pdfConfig.put("pageSize", pageSize);

	            pdfConfig.put("orientation", orientation);

	            body.add(
	                    "payload",
	                    mapper.writeValueAsString(pdfConfig)
	            );

	            HttpEntity<MultiValueMap<String, Object>>
	                    requestEntity =
	                    new HttpEntity<>(body, headers);

	            String apiUrl =
	                    "http://192.168.0.188:3012/api/v1/s3Upload/uploadHTML5";

	            ResponseEntity<byte[]> response =
	                    restTemplate.exchange(
	                            apiUrl,
	                            HttpMethod.POST,
	                            requestEntity,
	                            byte[].class
	                    );

	            if (response.getBody() == null
	                    || response.getBody().length == 0) {

	                throw new IOException("Empty PDF response");
	            }

	            // UPDATED
	            byte[] pdfBytes = response.getBody();

	            // PASSWORD PROTECTION
	            if (!passwordFields.isEmpty()) {

	                StringBuilder pwBuilder =
	                        new StringBuilder();

	                for (String pwExpr : passwordFields) {

	                    String pwValue;

	                    if (pwExpr.contains(".")) {

	                        String fullPath =
	                                userKey + "." + pwExpr.trim();

	                        pwValue =
	                                resolveFieldValueWithIndexes(
	                                        normalizedFieldMap,
	                                        fullPath
	                                );

	                    } else {

	                        pwValue = pwExpr.trim();
	                    }

	                    if (pwValue != null) {

	                        pwBuilder.append(
	                                pwValue.trim()
	                        );
	                    }
	                }

	                String userPassword =
	                        pwBuilder.toString().trim();

	                if (!userPassword.isEmpty()) {

	                    try (
	                            PDDocument document =
	                                    PDDocument.load(pdfBytes);

	                            ByteArrayOutputStream securedOut =
	                                    new ByteArrayOutputStream()
	                    ) {

	                        AccessPermission permissions =
	                                new AccessPermission();

	                        StandardProtectionPolicy policy =
	                                new StandardProtectionPolicy(
	                                        userPassword,
	                                        userPassword,
	                                        permissions
	                                );

	                        policy.setEncryptionKeyLength(128);

	                        policy.setPermissions(permissions);

	                        document.protect(policy);

	                        document.save(securedOut);

	                        pdfBytes = securedOut.toByteArray();
	                    }
	                }
	            }

	            // UPDATED
	            pdfMap.put(
	                    fileType + ".pdf",
	                    pdfBytes
	            );

	            repository.save(
	                    RecordEntity.builder()
	                            .fileName(fileType + ".pdf")
	                            .build()
	            );

	        } catch (Exception ex) {

	            throw new IOException(
	                    "PDF generation failed: "
	                            + ex.getMessage()
	            );
	        }
	    }

	    return pdfMap;
	}

	

	public byte[] createZipFromFiles(
	        Map<String, byte[]> pdfMap) throws IOException {

	    ByteArrayOutputStream baos =
	            new ByteArrayOutputStream();

	    try (ZipOutputStream zos =
	                 new ZipOutputStream(baos)) {

	        for (Map.Entry<String, byte[]> entry
	                : pdfMap.entrySet()) {

	            ZipEntry zipEntry =
	                    new ZipEntry(entry.getKey());

	            zos.putNextEntry(zipEntry);

	            zos.write(entry.getValue());

	            zos.closeEntry();
	        }
	    }

	    return baos.toByteArray();
	}
	@Data
	@AllArgsConstructor
	private static class GeneratedPdf {
		private String fileName;
		private byte[] bytes;
	}



	public Map<String, byte[]> processAndGenerateHtml(
	        String payloadJson,
	        MultipartFile[] files,
	        MultipartFile htmlFile) throws Exception {

	    Date startTime = new Date();

	    ObjectMapper mapper = new ObjectMapper();

	    JsonNode payloadNode;

	    try {

	        payloadNode = mapper.readTree(payloadJson);

	    } catch (Exception e) {

	        logToDatabase(
	                null,
	                "FAILURE",
	                "JSON parsing error: " + e.getMessage(),
	                startTime
	        );

	        throw e;
	    }

	    Map<String, JsonNode> htmlIdToJsonField =
	            new LinkedHashMap<>();

	    for (JsonNode obj : payloadNode) {

	        obj.fields().forEachRemaining(entry ->
	                htmlIdToJsonField.put(
	                        entry.getKey(),
	                        entry.getValue()
	                ));
	    }

	    List<String> fileNameFields =
	            new ArrayList<>();

	    JsonNode fileNameNode =
	            htmlIdToJsonField.get("file_name");

	    if (fileNameNode != null) {

	        if (fileNameNode.isTextual()) {

	            fileNameFields.addAll(
	                    Arrays.asList(
	                            fileNameNode.asText().split(",")
	                    )
	            );

	        } else if (fileNameNode.isArray()) {

	            fileNameNode.forEach(
	                    n -> fileNameFields.add(n.asText())
	            );
	        }
	    }

	    List<String> passwordFields =
	            new ArrayList<>();

	    JsonNode passwordNode =
	            htmlIdToJsonField.get("password");

	    if (passwordNode != null) {

	        if (passwordNode.isTextual()) {

	            passwordFields.addAll(
	                    Arrays.asList(
	                            passwordNode.asText().split(",")
	                    )
	            );

	        } else if (passwordNode.isArray()) {

	            passwordNode.forEach(
	                    n -> passwordFields.add(n.asText())
	            );
	        }
	    }

	    String htmlTemplate =
	            new String(
	                    htmlFile.getBytes(),
	                    StandardCharsets.UTF_8
	            ).replaceFirst("^\uFEFF", "");

	    // UPDATED
	    Map<String, byte[]> htmlMap =
	            new LinkedHashMap<>();

	    for (MultipartFile file : files) {

	        JsonNode dataJson =
	                mapper.readTree(file.getInputStream());

	        for (Iterator<Map.Entry<String, JsonNode>> users =
	                dataJson.fields();
	             users.hasNext(); ) {

	            Map.Entry<String, JsonNode> entry =
	                    users.next();

	            String userKey = entry.getKey();

	            JsonNode userNode = entry.getValue();

	            Map<String, JsonNode> normalizedFieldMap =
	                    new HashMap<>();

	            normalizedFieldMap.put(
	                    userKey.toLowerCase(),
	                    userNode
	            );

	            userNode.fieldNames().forEachRemaining(field ->
	                    normalizedFieldMap.put(
	                            field.toLowerCase(),
	                            userNode.get(field)
	                    )
	            );

	            Document doc = Jsoup.parse(htmlTemplate);

	            AtomicBoolean hasValidData =
	                    new AtomicBoolean(false);

	            doc.outputSettings().syntax(
	                    Document.OutputSettings.Syntax.xml
	            );

	            htmlIdToJsonField.forEach((id, nodeRef) -> {

	                String fieldRef =
	                        nodeRef.isTextual()
	                                ? nodeRef.asText()
	                                : null;

	                if (fieldRef == null)
	                    return;

	                String fullPath = fieldRef;

	                if (!fieldRef.startsWith(userKey + ".")) {

	                    fullPath =
	                            userKey + "." + fieldRef;
	                }

	                String value =
	                        resolveFieldValueWithIndexes(
	                                normalizedFieldMap,
	                                fullPath.trim()
	                        );

	                Element elem =
	                        doc.getElementById(id);

	                if (elem != null) {

	                    if (value != null
	                            && !value.isEmpty()) {

	                        elem.text(value);

	                        hasValidData.set(true);
	                    }
	                }
	            });

	            String fileType;

	            if (!fileNameFields.isEmpty()) {

	                StringBuilder fnBuilder =
	                        new StringBuilder();

	                for (String fnExpr : fileNameFields) {

	                    String fnValue =
	                            resolveFieldValueWithIndexes(
	                                    normalizedFieldMap,
	                                    userKey + "."
	                                            + fnExpr.trim()
	                            );

	                    if (fnValue != null
	                            && !fnValue.isEmpty()) {

	                        fnBuilder.append(fnValue);

	                    } else {

	                        fnBuilder.append("file_")
	                                .append(UUID.randomUUID())
	                                .append("_");
	                    }
	                }

	                fileType =
	                        fnBuilder.toString()
	                                .replaceAll("_$", "");

	            } else {

	                fileType =
	                        "file_" + userKey + "_"
	                                + UUID.randomUUID();
	            }

	            // JSON injection
	            String userJsonStr =
	                    mapper.writeValueAsString(dataJson);

	            String injectionScript = String.format(
	                    "<script id='__injected_json__'>"
	                            + "(function(){try{"
	                            + "var d=%s;"
	                            + "var n='%s';"
	                            + "localStorage.setItem('common_json',JSON.stringify(d));"
	                            + "localStorage.setItem('common_json_'+n,JSON.stringify(d));"
	                            + "localStorage.setItem('common_json_files',n);"
	                            + "}catch(e){}})();"
	                            + "</script>",
	                    userJsonStr,
	                    fileType
	            );

	            doc.head().prepend(injectionScript);

	            String userPassword = null;

	            if (!passwordFields.isEmpty()) {

	                StringBuilder pwBuilder =
	                        new StringBuilder();

	                for (String pwExpr : passwordFields) {

	                    String pwValue =
	                            resolveFieldValueWithIndexes(
	                                    normalizedFieldMap,
	                                    userKey + "."
	                                            + pwExpr.trim()
	                            );

	                    pwBuilder.append(
	                            pwValue != null
	                                    ? pwValue
	                                    : pwExpr.trim()
	                    );
	                }

	                userPassword =
	                        pwBuilder.toString();
	            }

	            String finalHtml = doc.outerHtml();

	            String encryptionKey =
	                    (userPassword != null
	                            && !userPassword.isEmpty())
	                            ? userPassword
	                            : "AutoEncryptHTMLFixedKey";

	            String encryptedFullHtml =
	                    encryptAES(
	                            finalHtml,
	                            encryptionKey
	                    );

	            StringBuilder decryptWrapper =
	                    new StringBuilder();

	            decryptWrapper.append(
	                    "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Encrypted Page</title></head><body>"
	            );

	            decryptWrapper.append(
	                    "<div id='encrypted-content' style='display:none;'>"
	            );

	            decryptWrapper.append(encryptedFullHtml);

	            decryptWrapper.append("</div>");

	            decryptWrapper.append("<script>");

	            decryptWrapper.append(
	                    "async function decryptAES(encryptedBase64,keyString){"
	            );

	            decryptWrapper.append(
	                    "function base64ToArrayBuffer(base64){"
	            );

	            decryptWrapper.append(
	                    "var binary_string=atob(base64);"
	            );

	            decryptWrapper.append(
	                    "var len=binary_string.length;"
	            );

	            decryptWrapper.append(
	                    "var bytes=new Uint8Array(len);"
	            );

	            decryptWrapper.append(
	                    "for(var i=0;i<len;i++)bytes[i]=binary_string.charCodeAt(i);"
	            );

	            decryptWrapper.append(
	                    "return bytes;}"
	            );

	            decryptWrapper.append(
	                    "const encryptedBytes=base64ToArrayBuffer(encryptedBase64);"
	            );

	            decryptWrapper.append(
	                    "const iv=encryptedBytes.slice(0,12);"
	            );

	            decryptWrapper.append(
	                    "const data=encryptedBytes.slice(12);"
	            );

	            decryptWrapper.append(
	                    "const keyBytes=new Uint8Array(32);"
	            );

	            decryptWrapper.append(
	                    "const passwordBytes=new TextEncoder().encode(keyString);"
	            );

	            decryptWrapper.append(
	                    "keyBytes.set(passwordBytes.slice(0,Math.min(32,passwordBytes.length)));"
	            );

	            decryptWrapper.append(
	                    "const cryptoKey=await crypto.subtle.importKey('raw',keyBytes,{name:'AES-GCM'},false,['decrypt']);"
	            );

	            decryptWrapper.append(
	                    "const decrypted=await crypto.subtle.decrypt({name:'AES-GCM',iv:iv},cryptoKey,data);"
	            );

	            decryptWrapper.append(
	                    "return new TextDecoder().decode(decrypted);}"
	            );

	            decryptWrapper.append("(async()=>{");

	            if (userPassword != null
	                    && !userPassword.isEmpty()) {

	                decryptWrapper.append(
	                        "try{"
	                                + "var pass=prompt('Enter password to view content:');"
	                                + "var decrypted=await decryptAES(document.getElementById('encrypted-content').textContent,pass);"
	                                + "document.open();document.write(decrypted);document.close();"
	                                + "}catch(e){document.body.innerHTML='<h2>Access Denied</h2>';}"
	                );

	            } else {

	                decryptWrapper.append(
	                        "try{"
	                                + "var decrypted=await decryptAES(document.getElementById('encrypted-content').textContent,'AutoEncryptHTMLFixedKey');"
	                                + "document.open();document.write(decrypted);document.close();"
	                                + "}catch(e){document.body.innerHTML='<h2>Decryption Error</h2>';}"
	                );
	            }

	            decryptWrapper.append("})();");

	            decryptWrapper.append("</script>");

	            decryptWrapper.append("</body></html>");

	            // UPDATED
	            byte[] htmlBytes =
	                    decryptWrapper.toString()
	                            .getBytes(StandardCharsets.UTF_8);

	            // UPDATED
	            htmlMap.put(
	                    fileType + ".html",
	                    htmlBytes
	            );
	        }
	    }

	    return htmlMap;
	}
	
	
	private String encryptAES(String plaintext, String password) throws Exception {
		byte[] keyBytes = Arrays.copyOf(password.getBytes(StandardCharsets.UTF_8), 32); // 256-bit key
		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

		byte[] iv = new byte[12];
		new SecureRandom().nextBytes(iv);
		GCMParameterSpec spec = new GCMParameterSpec(128, iv);

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);
		byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

		byte[] encryptedWithIv = new byte[iv.length + encrypted.length];
		System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
		System.arraycopy(encrypted, 0, encryptedWithIv, iv.length, encrypted.length);

		return Base64.getEncoder().encodeToString(encryptedWithIv);
	}

	private String extractBodyContent(String html) {
		int start = html.indexOf("<body");
		start = html.indexOf(">", start) + 1;
		int end = html.indexOf("</body>", start);
		return html.substring(start, end);
	}

	
	
	public void logToDatabase(RequestDTO request, String result, String errorMessage, Date startTime)
			throws SQLException {

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date endTime = new Date();

		LogData info = new LogData();
		info.setMessage(errorMessage);
		info.setResult(result);
		info.setSendRequestTime(startTime);
		info.setOutputResponseTime(endTime);
		logRepository.save(info);
	}

	


	public void processZipAndGenerateHtmlZip(
			String payloadJson,
			MultipartFile zipFile,
			MultipartFile htmlFile,
			OutputStream responseStream) throws Exception {

		Date startTime = new Date();
		ObjectMapper mapper = new ObjectMapper();
		JsonNode payloadNode;

		try {
			payloadNode = mapper.readTree(payloadJson);
		} catch (Exception e) {
			logToDatabase(null, "FAILURE", "JSON parsing error: " + e.getMessage(), startTime);
			throw e;
		}

		JsonNode mappingNode;
		if (payloadNode.isArray()) {
			mappingNode = payloadNode;
		} else if (payloadNode.has("mapping")) {
			mappingNode = payloadNode.get("mapping");
		} else {
			throw new IllegalArgumentException("Invalid payload format.");
		}

		Map<String, JsonNode> htmlIdToJsonField = new LinkedHashMap<>();
		for (JsonNode obj : mappingNode) {
			obj.fields().forEachRemaining(entry -> htmlIdToJsonField.put(entry.getKey(), entry.getValue()));
		}

		List<String> fileNameFields = extractTextList(htmlIdToJsonField.get("file_name"));
		List<String> passwordFields = extractTextList(htmlIdToJsonField.get("password"));
		String htmlTemplate = new String(htmlFile.getBytes(), StandardCharsets.UTF_8)
				.replaceFirst("^\uFEFF", "");
		Set<String> usedNames = new HashSet<>();
		StringBuilder generationErrors = new StringBuilder();
		int generatedCount = 0;

		try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream());
				ZipOutputStream zos = new ZipOutputStream(responseStream)) {

			zos.setLevel(java.util.zip.Deflater.BEST_SPEED);
			ZipEntry jsonEntry;
			while ((jsonEntry = zis.getNextEntry()) != null) {
				try {
					if (jsonEntry.isDirectory()
							|| !jsonEntry.getName().toLowerCase().endsWith(".json")) {
						continue;
					}

					byte[] jsonBytes = readCurrentZipEntry(zis);
					if (jsonBytes.length == 0) {
						continue;
					}

					JsonNode dataJson = mapper.readTree(jsonBytes);
					for (Iterator<Map.Entry<String, JsonNode>> users = dataJson.fields(); users.hasNext();) {
						Map.Entry<String, JsonNode> entry = users.next();
						String userKey = entry.getKey();
						JsonNode userNode = entry.getValue();
						Map<String, JsonNode> normalizedFieldMap = normalizeUserFields(userKey, userNode, dataJson);

						Document doc = Jsoup.parse(htmlTemplate);
						preserveHtmlOutput(doc);
						fillTemplate(doc, htmlIdToJsonField, normalizedFieldMap, userKey);

						String fileType = buildFileName(fileNameFields, normalizedFieldMap, userKey);
						String injectionScript = buildJsonInjectionScript(mapper, dataJson, fileType);
						doc.head().prepend(injectionScript);

						byte[] htmlBytes = doc.outerHtml()
								.getBytes(StandardCharsets.UTF_8);

						String htmlName = uniqueZipEntryName(sanitizeFileName(fileType), "html", usedNames);
						zos.putNextEntry(new ZipEntry(htmlName));
						zos.write(htmlBytes);
						zos.closeEntry();
						zos.flush();
						generatedCount++;
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					generationErrors
							.append(jsonEntry != null ? jsonEntry.getName() : "unknown")
							.append(" -> ")
							.append(ex.getMessage())
							.append(System.lineSeparator());
				} finally {
					zis.closeEntry();
				}
			}

			// if (generatedCount == 0) {
			// 	if (generationErrors.length() == 0) {
			// 		generationErrors.append(
			// 				"No HTML generated. Check multipart keys: use file, payload, and jsonFile.");
			// 	}

			// 	zos.putNextEntry(new ZipEntry("_generation_errors.txt"));
			// 	zos.write(generationErrors.toString().getBytes(StandardCharsets.UTF_8));
			// 	zos.closeEntry();
			// }

			// zos.finish();
			// zos.flush();
			if (generatedCount == 0) {

    if (generationErrors.length() == 0) {
        generationErrors.append(
                "No PDF generated. Check that the ZIP contains valid .json files "
                        + "and that the payload mapping matches the JSON field names.");
    }

    zos.putNextEntry(new ZipEntry("_generation_errors.txt"));

    zos.write(
            generationErrors.toString().getBytes(StandardCharsets.UTF_8));

    zos.closeEntry();
}

/*
 * Batch save
 */
// if (!records.isEmpty()) {

//     repository.saveAll(records);
// }

zos.finish();

zos.flush();
		}
	}
	
	
	
	private String buildJsonInjectionScript(ObjectMapper mapper, JsonNode dataJson, String fileType)
			throws IOException {

		String userJsonStr = mapper.writeValueAsString(dataJson);
		String safeName = fileType.replace("\\", "\\\\").replace("'", "\\'");
		return String.format(
				"<script id='__injected_json__'>"
						+ "(function(){try{"
						+ "var d=%s;"
						+ "var n='%s';"
						+ "localStorage.setItem('common_json',JSON.stringify(d));"
						+ "localStorage.setItem('common_json_'+n,JSON.stringify(d));"
						+ "localStorage.setItem('common_json_files',n);"
						+ "}catch(e){}})();"
						+ "</script>",
				userJsonStr,
				safeName);
	}

	private void preserveHtmlOutput(Document doc) {
		doc.outputSettings()
				.syntax(Document.OutputSettings.Syntax.html)
				.prettyPrint(false)
				.charset(StandardCharsets.UTF_8);
	}



}
