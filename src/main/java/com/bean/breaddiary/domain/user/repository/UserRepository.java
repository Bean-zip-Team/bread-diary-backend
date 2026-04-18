package com.bean.breaddiary.domain.user.repository;

import com.bean.breaddiary.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
