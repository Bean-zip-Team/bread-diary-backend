package com.bean.breaddiary.domain.breadtype.repository;

import com.bean.breaddiary.domain.breadtype.entity.BreadType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BreadTypeRepository extends JpaRepository<BreadType, Long> {

    Optional<BreadType> findByCode(String code);

    Optional<BreadType> findByName(String name);

    List<BreadType> findAllByOrderByIdAsc();
}
