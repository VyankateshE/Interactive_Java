package com.example.InteractiveDesignBackend.Repositor;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.InteractiveDesignBackend.Entity.RecordEntity;

public interface RecordRepository extends JpaRepository<RecordEntity, Long> {
}
