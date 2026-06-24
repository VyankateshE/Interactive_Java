package com.example.InteractiveDesignBackend.Repositor;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.InteractiveDesignBackend.Entity.User;

import java.util.Optional;

public interface Repository extends JpaRepository<User, Integer> {
	Optional<User> findByName(String name);

	Optional<User> findById(Integer id);
}
