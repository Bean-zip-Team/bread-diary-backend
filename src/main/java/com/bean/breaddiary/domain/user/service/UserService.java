package com.bean.breaddiary.domain.user.service;

import com.bean.breaddiary.domain.breadrecord.repository.BreadRecordRepository;
import com.bean.breaddiary.domain.user.dto.mapper.UserMapper;
import com.bean.breaddiary.domain.user.dto.response.UserMeResponse;
import com.bean.breaddiary.domain.user.dto.response.UserStatsResponse;
import com.bean.breaddiary.domain.user.entity.User;
import com.bean.breaddiary.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final BreadRecordRepository breadRecordRepository;
    private final UserMapper userMapper;

    public UserMeResponse getCurrentUserProfile(UUID userId) {
        User user = getUserById(userId);
        UserStatsResponse stats = getUserStats(userId);

        return createUserMeResponse(user, stats);
    }

    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 사용자를 찾을 수 없습니다."
                ));
    }

    public UserStatsResponse getUserStats(UUID userId) {
        return userMapper.mapToUserStatsResponse(
                breadRecordRepository.findUserStatsByUserId(userId)
        );
    }

    public UserMeResponse createUserMeResponse(User user, UserStatsResponse stats) {
        return userMapper.mapToUserMeResponse(user, stats);
    }
}
