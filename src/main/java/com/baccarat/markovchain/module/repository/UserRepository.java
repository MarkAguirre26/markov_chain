package com.baccarat.markovchain.module.repository;

import com.baccarat.markovchain.module.data.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    User findByUsername(String username);
    User findByUsernameAndIsActive(String username, int isActive);
   User findByEmailAndIsActive(String email, int isActive);
    User findByEmail(String email);
    User findUserByUsernameAndEmail(String username, String email);

}