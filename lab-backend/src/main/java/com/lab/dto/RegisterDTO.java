package com.lab.dto;

import lombok.Data;



@Data
public class RegisterDTO {

    private String username;

    private String password;
    private String realName;

    private String role;  // TEACHER, STUDENT
    private String phone;
    private String gender;
    private String email;
}
