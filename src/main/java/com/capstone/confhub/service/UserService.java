package com.capstone.confhub.service;

import com.capstone.confhub.dto.UserDTO;
import com.capstone.confhub.dto.response.UserResponseDTO;
import com.capstone.confhub.dto.response.PagedResponse;

public interface UserService {
    UserResponseDTO createUser(UserDTO userDTO);
    UserResponseDTO updateUser(Integer id, UserDTO userDTO);
    UserResponseDTO getUserById(Integer id);
    PagedResponse<UserResponseDTO> getAllUsers(int page, int size);
    void deleteUser(Integer id);
    UserResponseDTO getUserByEmail(String email);
}