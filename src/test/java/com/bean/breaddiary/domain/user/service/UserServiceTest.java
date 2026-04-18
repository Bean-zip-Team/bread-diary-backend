package com.bean.breaddiary.domain.user.service;

import com.bean.breaddiary.domain.breadrecord.dto.projection.UserStatsProjection;
import com.bean.breaddiary.domain.breadrecord.repository.BreadRecordRepository;
import com.bean.breaddiary.domain.user.dto.mapper.UserMapper;
import com.bean.breaddiary.domain.user.dto.response.UserMeResponse;
import com.bean.breaddiary.domain.user.dto.response.UserStatsResponse;
import com.bean.breaddiary.domain.user.entity.User;
import com.bean.breaddiary.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private BreadRecordRepository breadRecordRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        breadRecordRepository = mock(BreadRecordRepository.class);
        UserMapper userMapper = Mappers.getMapper(UserMapper.class);
        userService = new UserService(userRepository, breadRecordRepository, userMapper);
    }

    @Test
    void getCurrentUserProfileCombinesUserAndStats() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        User user = User.builder()
                .id(userId)
                .nickname("breadlover")
                .email("bread@toss.im")
                .profileImageUrl("https://cdn.breaddex.app/profiles/550e8400.webp")
                .bio(null)
                .createdAt(LocalDateTime.of(2026, 4, 1, 0, 0))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(breadRecordRepository.findUserStatsByUserId(userId))
                .thenReturn(createProjection(42L, 18L, 4.25));

        UserMeResponse actual = userService.getCurrentUserProfile(userId);

        assertEquals(userId, actual.getId());
        assertEquals("breadlover", actual.getNickname());
        assertEquals("bread@toss.im", actual.getEmail());
        assertNotNull(actual.getStats());
        assertEquals(42L, actual.getStats().getTotalRecords());
        assertEquals(18L, actual.getStats().getUniqueShops());
        assertEquals(4.3, actual.getStats().getAvgRating());

        verify(userRepository).findById(userId);
        verify(breadRecordRepository).findUserStatsByUserId(userId);
    }

    @Test
    void getUserStatsNormalizesNullCountsAndRatingToZero() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        when(breadRecordRepository.findUserStatsByUserId(userId))
                .thenReturn(createProjection(null, null, null));

        UserStatsResponse actual = userService.getUserStats(userId);

        assertEquals(0L, actual.getTotalRecords());
        assertEquals(0L, actual.getUniqueShops());
        assertEquals(0.0, actual.getAvgRating());
    }

    @Test
    void getUserByIdThrowsNotFoundWhenUserDoesNotExist() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.getUserById(userId)
        );

        assertEquals(404, exception.getStatusCode().value());
        assertNotNull(exception.getReason());
    }

    private UserStatsProjection createProjection(Long totalRecords, Long uniqueShops, Double avgRating) {
        return new UserStatsProjection() {
            @Override
            public Long getTotalRecords() {
                return totalRecords;
            }

            @Override
            public Long getUniqueShops() {
                return uniqueShops;
            }

            @Override
            public Double getAvgRating() {
                return avgRating;
            }
        };
    }
}
