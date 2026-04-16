package com.bean.breaddiary.domain.bread.service;

import com.bean.breaddiary.domain.bread.dto.mapper.BreadMapper;
import com.bean.breaddiary.domain.bread.entity.Bread;
import com.bean.breaddiary.domain.bread.repository.BreadRepository;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateNewBreadRecordRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BreadService {

    private static final String DEFAULT_USER_BREAD_IMAGE_URL =
            "https://cdn.bread-diary.app/catalog/default_user_bread.webp";

    private final BreadRepository breadRepository;
    private final BreadMapper breadMapper;

    public Bread getBreadById(UUID breadId) {
        return breadRepository.findById(breadId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 빵을 카탈로그에서 찾을 수 없습니다."
                ));
    }

    public boolean existsByName(String name) {
        return breadRepository.existsByName(name);
    }

    @Transactional
    public Bread createUserBread(CreateNewBreadRecordRequest request, UUID userId) {
        validateBreadNameNotDuplicated(request.getName());

        Bread bread = breadMapper.mapToBread(
                request,
                getNextStickerNumber(),
                DEFAULT_USER_BREAD_IMAGE_URL,
                userId
        );

        return breadRepository.save(bread);
    }

    private Integer getNextStickerNumber() {
        return breadRepository.findTopByOrderByStickerNumberDesc()
                .map(Bread::getStickerNumber)
                .orElse(0) + 1;
    }

    private void validateBreadNameNotDuplicated(String name) {
        if (existsByName(name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "같은 이름의 빵이 이미 카탈로그에 있습니다."
            );
        }
    }
}
