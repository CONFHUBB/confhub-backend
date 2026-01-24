package com.capstone.confms.service;

import com.capstone.confms.dto.UserDTO;
import com.capstone.confms.dto.response.UserResponseDTO;
import java.util.List;

public interface UserService {
    UserResponseDTO createUser(UserDTO userDTO);
    UserResponseDTO updateUser(Integer id, UserDTO userDTO);
    UserResponseDTO getUserById(Integer id);
    List<UserResponseDTO> getAllUsers();
    void deleteUser(Integer id);
    UserResponseDTO getUserByEmail(String email);
}