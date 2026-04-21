package com.bean.breaddiary.domain.breadtype.service;

import com.bean.breaddiary.domain.breadtype.dto.mapper.BreadTypeMapper;
import com.bean.breaddiary.domain.breadtype.dto.response.BreadTypeListResponse;
import com.bean.breaddiary.domain.breadtype.entity.BreadType;
import com.bean.breaddiary.domain.breadtype.repository.BreadTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BreadTypeService {

    private final BreadTypeRepository breadTypeRepository;
    private final BreadTypeMapper breadTypeMapper;

    public BreadType getBreadTypeByCode(String code) {
        String normalizedCode = normalizeCode(code);

        return breadTypeRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "올바른 빵 종류를 선택해주세요."
                ));
    }

    public BreadType getBreadTypeByName(String name) {
        String normalizedName = requireName(name);

        return breadTypeRepository.findByName(normalizedName)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "빵 종류를 찾을 수 없습니다: " + normalizedName
                ));
    }

    public BreadTypeListResponse getBreadTypes() {
        return breadTypeMapper.mapToBreadTypeListResponse(
                breadTypeRepository.findAllByOrderByIdAsc()
        );
    }

    private String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "올바른 빵 종류를 선택해주세요."
            );
        }

        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String requireName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "빵 종류 값이 비어 있습니다."
            );
        }

        return name.trim();
    }
}
