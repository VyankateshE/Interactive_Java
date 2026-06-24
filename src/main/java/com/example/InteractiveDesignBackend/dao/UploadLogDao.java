package com.example.InteractiveDesignBackend.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Optional;

@Repository
public class UploadLogDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Save a new log entry and return the generated upload_id
    public int saveLog(String fileName, String sheetName, int totalRows) {
        String sql = "INSERT INTO upload_logs (file_name, sheet_name, total_rows, uploaded_at) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, fileName, sheetName, totalRows, new Timestamp(System.currentTimeMillis()));

        // Get the last inserted ID
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    /**
     * Checks if a file with the given name already exists in the upload_logs table.
     * @param fileName The name of the file to check.
     * @return true if the file exists, false otherwise.
     */
    public boolean fileExists(String fileName) {
        String sql = "SELECT COUNT(*) FROM upload_logs WHERE file_name = ?";
        // queryForObject(sql, type, args) is used when expecting a single result (like COUNT)
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, fileName);
        return count != null && count > 0;
    }

    /**
     * Retrieves the file_name from upload_logs based on upload_id.
     * @param uploadId The upload ID.
     * @return An Optional containing the file name if found, otherwise empty.
     */
    public Optional<String> getFileNameByUploadId(int uploadId) {
        String sql = "SELECT file_name FROM upload_logs WHERE upload_id = ? LIMIT 1";
        try {
            String fileName = jdbcTemplate.queryForObject(sql, String.class, uploadId);
            return Optional.ofNullable(fileName);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // No result found for the given uploadId
            return Optional.empty();
        }
    }
}
