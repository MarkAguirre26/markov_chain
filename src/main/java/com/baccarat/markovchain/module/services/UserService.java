package com.baccarat.markovchain.module.services;

import com.baccarat.markovchain.module.model.User;

import java.util.List;

public interface UserService {

    User findUserByUsername(String username);
    User createUser(User user);
    User updateUser(User user);
    void deleteUser(Integer userId);
    User getUserById(Integer userId);
    List<User> getAllUsers();
}
