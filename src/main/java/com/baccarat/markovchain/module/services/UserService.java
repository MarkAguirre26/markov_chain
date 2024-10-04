package com.baccarat.markovchain.module.services;

import com.baccarat.markovchain.module.data.User;

import java.util.List;

public interface UserService {

    User findUserByUsername(String username);

    User findUserByEmail(String email);

    User findByUsernameAndIsActive(String username, int isActive);

    User createUser(User user);

    User updateUser(User user);

    void deleteUser(Integer userId);

    User getUserById(Integer userId);

    List<User> getAllUsers();
}
