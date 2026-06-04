package com.example.InteractiveDesignBackend.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.InteractiveDesignBackend.Entity.User;
import com.example.InteractiveDesignBackend.Repositor.Repository;

@Service
public class JsonService {

	@Autowired
	private Repository crud_repository;

//    public String addUser(UserDTO userDTO){
//        User user = UserConvertor.convertUserDTOtoEntity(userDTO);
//        crud_repository.save(user);
//        return "User added successfully";
//        return "User added successfullyss";
//    }

	public String editUser(Integer id, User user) {
		User updatedUser = crud_repository.findById(id).get();
		if (user.getName() != null)
			updatedUser.setName(user.getName());
		if (user.getEditableHtml() != null)
			updatedUser.setEditableHtml(user.getEditableHtml());
		if (user.getDownloadableHtml() != null)
			updatedUser.setDownloadableHtml(user.getDownloadableHtml());
		crud_repository.save(updatedUser);
		return "Template updated successfully";
	}

	public String deleteUser(Integer id) {
		User user1 = crud_repository.findById(id).get();
		crud_repository.delete(user1);
		return "Template with id " + id + " Deleted successfully";
	}
}
