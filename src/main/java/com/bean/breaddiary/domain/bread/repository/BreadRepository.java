package com.bean.breaddiary.domain.bread.repository;

import com.bean.breaddiary.domain.bread.entity.Bread;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BreadRepository extends JpaRepository<Bread, UUID> {

    Optional<Bread> findByName(String name);

    Optional<Bread> findByStickerNumber(Integer stickerNumber);

    Optional<Bread> findTopByOrderByStickerNumberDesc();

    boolean existsByName(String name);
}
