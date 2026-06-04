package com.example.InteractiveDesignBackend.dao;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;

@Repository
public class ExcelRowDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Save data without file_name and sheet_name in this table
    public void saveData(List<Map<String, Object>> rows, int uploadId) {
        String sql = "INSERT INTO uploaded_excel_data (data, upload_id) VALUES (?, ?)";

        for (Map<String, Object> row : rows) {
            String jsonData = toJson(row);
            jdbcTemplate.update(sql, jsonData, uploadId);
        }
    }

    // Converts a map to a JSON string using ObjectMapper for robustness
    private String toJson(Map<String, Object> row) {
        try {
            return objectMapper.writeValueAsString(row);
        } catch (JsonProcessingException e) {
            System.err.println("Error converting map to JSON: " + e.getMessage());
            return "{}"; // Fallback to empty JSON in case of error
        }
    }

    public Set<String> getHeadersByUploadId(int uploadId) {
        String sql = "SELECT data FROM uploaded_excel_data WHERE upload_id = ? LIMIT 1";

        List<String> results = jdbcTemplate.queryForList(sql, String.class, uploadId);

        if (!results.isEmpty()) {
            String json = results.get(0);
            try {
                Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                return map.keySet();
            } catch (Exception e) {
                System.err.println("Error parsing JSON for headers: " + e.getMessage());
                return new HashSet<>();
            }
        }
        return new HashSet<>();
    }

    public Set<String> findUniqueValuesForColumn(int uploadId, String columnName) {
        Set<String> uniqueValues = new HashSet<>();
      
        String sql = "SELECT DISTINCT JSON_UNQUOTE(JSON_EXTRACT(data, '$.\"" + columnName + "\"')) FROM uploaded_excel_data WHERE upload_id = ?";

        try {
            List<String> results = jdbcTemplate.queryForList(sql, String.class, uploadId);

            for (String value : results) {
            
                if (value != null && !value.trim().equalsIgnoreCase("null") && !value.trim().isEmpty()) {
                    uniqueValues.add(value);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching unique values for column '" + columnName + "': " + e.getMessage());
            return new HashSet<>();
        }
        return uniqueValues;
    }

    public Map<String, String> findValuesByCriteriaByUploadId(int uploadId, Map<String, String> criteria, List<String> targetColumns) {
        StringBuilder sqlBuilder = new StringBuilder("SELECT data FROM uploaded_excel_data WHERE upload_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(uploadId);

        System.out.println(criteria.toString());
        if (criteria != null && !criteria.isEmpty()) {
            for (Map.Entry<String, String> entry : criteria.entrySet()) {
                String colName = entry.getKey();
                String colValue = entry.getValue();
             
                sqlBuilder.append(" AND JSON_UNQUOTE(JSON_EXTRACT(data, '$.\"")
                          .append(colName)
                          .append("\"')) = ?");
                params.add(colValue);
            }
        }
        sqlBuilder.append(" LIMIT 1"); 

        List<String> results = jdbcTemplate.queryForList(sqlBuilder.toString(), String.class, params.toArray());

        if (!results.isEmpty()) {
            String json = results.get(0);
            try {
             
            	objectMapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

                Map<String, Object> fullRecord = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                Map<String, String> extractedValues = new HashMap<>();

             
                if (targetColumns == null || targetColumns.isEmpty()) {
                    for (Map.Entry<String, Object> entry : fullRecord.entrySet()) {
                    
                        extractedValues.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null);
                    }
                } else {
                  
                    for (String targetColumn : targetColumns) {
                        if (fullRecord.containsKey(targetColumn)) {
                            extractedValues.put(targetColumn, fullRecord.get(targetColumn) != null ? fullRecord.get(targetColumn).toString() : null);
                        } else {
                          
                            extractedValues.put(targetColumn, null);
                        }
                    }
                }
                return extractedValues;
            } catch (Exception e) {
                System.err.println("Error parsing JSON for dynamic query: " + e.getMessage());
                return Collections.emptyMap();
            }
        }
        return Collections.emptyMap(); 
    }
}