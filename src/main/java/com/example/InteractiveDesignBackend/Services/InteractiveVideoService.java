package com.example.InteractiveDesignBackend.Services;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.InteractiveDesignBackend.dao.ExcelRowDao;
import com.example.InteractiveDesignBackend.dao.UploadLogDao;
import com.example.InteractiveDesignBackend.util.InteractiveVideoExcelReader;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

@Service
public class InteractiveVideoService {

    @Autowired
    private ExcelRowDao excelRowDao;

    @Autowired 
    private UploadLogDao uploadLogDao;



    // Save Excel data and return uploadId
    public int saveExcelData(MultipartFile file) {
        String fileName = file.getOriginalFilename();

        Map<String, List<Map<String, Object>>> data = InteractiveVideoExcelReader.readFile(file);
        int uploadId = -1;

        for (Map.Entry<String, List<Map<String, Object>>> entry : data.entrySet()) {
            String sheetName = entry.getKey();
            List<Map<String, Object>> rows = entry.getValue();

            // Save log and get uploadId
            uploadId = uploadLogDao.saveLog(fileName, sheetName, rows.size());
            // Pass only rows and uploadId to saveData
            excelRowDao.saveData(rows, uploadId);
        }
        return uploadId;
    }

    // Get headers by uploadId
    public Set<String> getFileHeadersByUploadId(int uploadId) {
        return excelRowDao.getHeadersByUploadId(uploadId);
    }

    // Get unique values for a column
    public Set<String> getUniqueValuesForColumn(int uploadId, String columnName) {
        return excelRowDao.findUniqueValuesForColumn(uploadId, columnName);
    }

    // Get dynamic values (used by query-full-row-form)
    public Map<String, String> getDynamicValuesByUploadId(int uploadId, Map<String, String> criteria, List<String> targetColumns) {
        return excelRowDao.findValuesByCriteriaByUploadId(uploadId, criteria, targetColumns);
    }

   
}