package com.backend.users;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.backend.users", "com.backend.core"})
public class BackendUsersManagementApplication {
  public static void main(String[] args) {
    SpringApplication.run(BackendUsersManagementApplication.class, args);
  }
}
