package com.health.diet.repository;

import com.health.diet.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/* 用户数据访问层 */
public interface UserRepository extends JpaRepository<User, Long> {

    /* findByUsername 按用户名查询 */
    Optional<User> findByUsername(String username);
}
