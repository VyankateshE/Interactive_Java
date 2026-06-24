package com.example.InteractiveDesignBackend.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.dao.EmptyResultDataAccessException; 

@Repository
public class TempDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

  
    public int insertJsonIntoTemp(String jsonData, int originalUploadId) {
        String sql = "INSERT INTO temp (upload_id, data) VALUES (?, ?)";
        return jdbcTemplate.update(sql, originalUploadId, jsonData);
    }

   
    public String getJsonDataFromTempById(int id) {
        String sql = "SELECT data FROM temp WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, id);
        } catch (EmptyResultDataAccessException e) {
        
            return null;
        }
    }

   
    public int deleteRowFromTempById(int id) {
        String sql = "DELETE FROM temp WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }
}
