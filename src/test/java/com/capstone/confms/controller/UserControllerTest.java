package com.capstone.confms.controller;

import com.capstone.confms.dto.UserDTO;
import com.capstone.confms.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController(userService);
    }

    @Test
    void shouldCreateController() {
        assertNotNull(userController);
    }

    @Test
    void createUserShouldReturnCreated() {
        UserDTO userDTO = new UserDTO();
        var response = mock(com.capstone.confms.dto.response.UserResponseDTO.class);
        when(userService.createUser(userDTO)).thenReturn(response);

        var result = userController.createUser(userDTO);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }
}
