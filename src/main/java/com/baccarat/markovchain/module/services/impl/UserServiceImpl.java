package com.baccarat.markovchain.module.services.impl;

import com.baccarat.markovchain.module.data.User;
import com.baccarat.markovchain.module.repository.UserRepository;
import com.baccarat.markovchain.module.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {


    private final UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User createUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User findByUsernameAndIsActive(String username, int isActive) {
        return userRepository.findByUsernameAndIsActive(username, isActive);
    }


    @Override
    public User findByEmailAndIsActiveAndLogged(String email, int isActive, int logged) {
        return userRepository.findByEmailAndIsActiveAndLogged(email, isActive, logged);
    }


    @Override
    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }


    @Override
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Integer userId) {
        userRepository.deleteById(userId);
    }

    @Override
    public User getUserById(Integer userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.orElse(null); // or throw an exception
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
