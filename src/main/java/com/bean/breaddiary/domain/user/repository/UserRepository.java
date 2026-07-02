package com.bean.breaddiary.domain.user.repository;

import com.bean.breaddiary.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    Optional<User> findByTossUserKeyAndDeletedAtIsNull(String tossUserKey);

    Optional<User> findByTossUserKey(String tossUserKey);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByEmail(String email);
}
