package com.bean.breaddiary.domain.breadrecord.controller;

import com.bean.breaddiary.domain.bread.entity.BreadType;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateNewBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.request.UpdateBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.response.BreadRecordCreateResponse;
import com.bean.breaddiary.domain.breadrecord.dto.response.BreadRecordDeleteResponse;
import com.bean.breaddiary.domain.breadrecord.dto.response.BreadRecordDetailResponse;
import com.bean.breaddiary.domain.breadrecord.service.BreadRecordComplexService;
import com.bean.breaddiary.global.interceptor.AuthRequestAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BreadRecordControllerContractTest {

    private static final UUID AUTHENTICATED_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RECORD_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID BREAD_ID = UUID.fromString("b0e1f2a3-c4d5-6789-abcd-ef0123456789");

    private BreadRecordComplexService breadRecordComplexService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        breadRecordComplexService = mock(BreadRecordComplexService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BreadRecordController(breadRecordComplexService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(snakeCaseObjectMapper()))
                .build();
    }

    @Test
    void createBreadRecordBindsSnakeCaseMultipartFieldsAndSerializesSnakeCaseResponse() throws Exception {
        UUID breadId = UUID.fromString("b0e1f2a3-c4d5-6789-abcd-ef0123456789");
        BreadRecordCreateResponse response = createResponse(breadId, BreadType.PASTRY, false);

        when(breadRecordComplexService.createBreadRecord(eq(AUTHENTICATED_USER_ID), any(CreateBreadRecordRequest.class)))
                .thenReturn(response);

        mockMvc.perform(multipart("/breads")
                        .requestAttr(AuthRequestAttributes.USER_ID, AUTHENTICATED_USER_ID)
                        .file(new MockMultipartFile(
                                "photo",
                                "croissant.webp",
                                MediaType.IMAGE_JPEG_VALUE,
                                "photo".getBytes()
                        ))
                        .param("bread_id", breadId.toString())
                        .param("shop_name", "루트브레드 성수점")
                        .param("eaten_date", "2026-03-14")
                        .param("rating", "5")
                        .param("review", "겉은 바삭하고 속이 촉촉해서 만족했어요"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bread_id").value(breadId.toString()))
                .andExpect(jsonPath("$.data.sticker_number").value(7))
                .andExpect(jsonPath("$.data.photo_url").value("https://cdn.bread-diary.app/bread-photos/record.webp"))
                .andExpect(jsonPath("$.data.photo_thumbnail_url").value("https://cdn.bread-diary.app/bread-photos/record_thumb.webp"))
                .andExpect(jsonPath("$.data.bread_type").value("PASTRY"))
                .andExpect(jsonPath("$.data.shop_name").value("루트브레드 성수점"))
                .andExpect(jsonPath("$.data.eaten_date").value("2026-03-14"))
                .andExpect(jsonPath("$.data.is_first_record").value(false))
                .andExpect(jsonPath("$.data.breadId").doesNotExist())
                .andExpect(jsonPath("$.data.shopName").doesNotExist());

        ArgumentCaptor<CreateBreadRecordRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateBreadRecordRequest.class);
        verify(breadRecordComplexService).createBreadRecord(eq(AUTHENTICATED_USER_ID), requestCaptor.capture());

        CreateBreadRecordRequest request = requestCaptor.getValue();
        assertEquals(breadId, request.getBreadId());
        assertEquals("루트브레드 성수점", request.getShopName());
        assertEquals(LocalDate.of(2026, 3, 14), request.getEatenDate());
    }

    @Test
    void createNewBreadRecordBindsSnakeCaseMultipartFields() throws Exception {
        UUID breadId = UUID.fromString("c1d2e3f4-a5b6-7890-cdef-ab0123456789");
        BreadRecordCreateResponse response = createResponse(breadId, BreadType.BAGEL, true);

        when(breadRecordComplexService.createNewBreadRecord(eq(AUTHENTICATED_USER_ID), any(CreateNewBreadRecordRequest.class)))
                .thenReturn(response);

        mockMvc.perform(multipart("/breads/new")
                        .requestAttr(AuthRequestAttributes.USER_ID, AUTHENTICATED_USER_ID)
                        .file(new MockMultipartFile(
                                "photo",
                                "bagel.webp",
                                MediaType.IMAGE_JPEG_VALUE,
                                "photo".getBytes()
                        ))
                        .param("name", "플레인 베이글")
                        .param("bread_type", "BAGEL")
                        .param("shop_name", "베이글집")
                        .param("eaten_date", "2026-03-15")
                        .param("rating", "4")
                        .param("review", "쫀득했어요"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bread_id").value(breadId.toString()))
                .andExpect(jsonPath("$.data.bread_type").value("BAGEL"))
                .andExpect(jsonPath("$.data.is_first_record").value(true))
                .andExpect(jsonPath("$.data.breadType").doesNotExist());

        ArgumentCaptor<CreateNewBreadRecordRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateNewBreadRecordRequest.class);
        verify(breadRecordComplexService).createNewBreadRecord(eq(AUTHENTICATED_USER_ID), requestCaptor.capture());

        CreateNewBreadRecordRequest request = requestCaptor.getValue();
        assertEquals("플레인 베이글", request.getName());
        assertEquals(BreadType.BAGEL, request.getBreadType());
        assertEquals("베이글집", request.getShopName());
        assertEquals(LocalDate.of(2026, 3, 15), request.getEatenDate());
    }

    @Test
    void getBreadRecordReturnsSpecResponseWithSnakeCaseFields() throws Exception {
        BreadRecordDetailResponse response = createDetailResponse();

        when(breadRecordComplexService.getBreadRecord(AUTHENTICATED_USER_ID, RECORD_ID)).thenReturn(response);

        mockMvc.perform(get("/breads/{recordId}", RECORD_ID)
                        .requestAttr(AuthRequestAttributes.USER_ID, AUTHENTICATED_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(RECORD_ID.toString()))
                .andExpect(jsonPath("$.data.bread_id").value(BREAD_ID.toString()))
                .andExpect(jsonPath("$.data.sticker_number").value(7))
                .andExpect(jsonPath("$.data.name").value("크루아상"))
                .andExpect(jsonPath("$.data.bread_type").value("PASTRY"))
                .andExpect(jsonPath("$.data.bread_type_label").value("페이스트리"))
                .andExpect(jsonPath("$.data.image_url").value("https://cdn.bread-diary.app/breads/croissant.webp"))
                .andExpect(jsonPath("$.data.photo_url").value("https://cdn.bread-diary.app/bread-photos/record.webp"))
                .andExpect(jsonPath("$.data.photo_thumbnail_url").value("https://cdn.bread-diary.app/bread-photos/record_thumb.webp"))
                .andExpect(jsonPath("$.data.shop_name").value("루트브레드 성수점"))
                .andExpect(jsonPath("$.data.eaten_date").value("2026-03-14"))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.review").value("겉은 바삭하고 속이 촉촉해서 만족했어요"))
                .andExpect(jsonPath("$.data.created_at").value("2026-03-14T09:30:00"))
                .andExpect(jsonPath("$.data.updated_at").value("2026-03-14T10:15:00"))
                .andExpect(jsonPath("$.data.breadId").doesNotExist());

        verify(breadRecordComplexService).getBreadRecord(AUTHENTICATED_USER_ID, RECORD_ID);
    }

    @Test
    void updateBreadRecordBindsSnakeCaseMultipartFields() throws Exception {
        BreadRecordDetailResponse response = createDetailResponse();

        when(breadRecordComplexService.updateBreadRecord(eq(AUTHENTICATED_USER_ID), eq(RECORD_ID), any(UpdateBreadRecordRequest.class)))
                .thenReturn(response);

        mockMvc.perform(multipart("/breads/{recordId}", RECORD_ID)
                        .requestAttr(AuthRequestAttributes.USER_ID, AUTHENTICATED_USER_ID)
                        .file(new MockMultipartFile(
                                "photo",
                                "croissant.webp",
                                MediaType.IMAGE_JPEG_VALUE,
                                "photo".getBytes()
                        ))
                        .param("shop_name", "")
                        .param("eaten_date", "2026-03-14")
                        .param("rating", "5")
                        .param("review", "")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(RECORD_ID.toString()))
                .andExpect(jsonPath("$.data.bread_id").value(BREAD_ID.toString()))
                .andExpect(jsonPath("$.data.photo_thumbnail_url").value("https://cdn.bread-diary.app/bread-photos/record_thumb.webp"));

        ArgumentCaptor<UpdateBreadRecordRequest> requestCaptor =
                ArgumentCaptor.forClass(UpdateBreadRecordRequest.class);
        verify(breadRecordComplexService).updateBreadRecord(eq(AUTHENTICATED_USER_ID), eq(RECORD_ID), requestCaptor.capture());

        UpdateBreadRecordRequest request = requestCaptor.getValue();
        assertEquals("", request.getShopName());
        assertEquals(LocalDate.of(2026, 3, 14), request.getEatenDate());
        assertEquals(5, request.getRating());
        assertEquals("", request.getReview());
    }

    @Test
    void deleteBreadRecordReturnsStickerRemoved() throws Exception {
        when(breadRecordComplexService.deleteBreadRecord(AUTHENTICATED_USER_ID, RECORD_ID))
                .thenReturn(new BreadRecordDeleteResponse(true));

        mockMvc.perform(delete("/breads/{recordId}", RECORD_ID)
                        .requestAttr(AuthRequestAttributes.USER_ID, AUTHENTICATED_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sticker_removed").value(true))
                .andExpect(jsonPath("$.data.stickerRemoved").doesNotExist());

        verify(breadRecordComplexService).deleteBreadRecord(AUTHENTICATED_USER_ID, RECORD_ID);
    }

    private BreadRecordCreateResponse createResponse(UUID breadId, BreadType breadType, boolean isFirstRecord) {
        return new BreadRecordCreateResponse(
                UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
                breadId,
                7,
                "https://cdn.bread-diary.app/bread-photos/record.webp",
                "https://cdn.bread-diary.app/bread-photos/record_thumb.webp",
                "크루아상",
                breadType,
                "https://cdn.bread-diary.app/breads/croissant.webp",
                "루트브레드 성수점",
                LocalDate.of(2026, 3, 14),
                5,
                "겉은 바삭하고 속이 촉촉해서 만족했어요",
                isFirstRecord,
                LocalDateTime.of(2026, 3, 14, 9, 30)
        );
    }

    private BreadRecordDetailResponse createDetailResponse() {
        return new BreadRecordDetailResponse(
                RECORD_ID,
                BREAD_ID,
                7,
                "크루아상",
                BreadType.PASTRY,
                "페이스트리",
                "https://cdn.bread-diary.app/breads/croissant.webp",
                "https://cdn.bread-diary.app/bread-photos/record.webp",
                "https://cdn.bread-diary.app/bread-photos/record_thumb.webp",
                "루트브레드 성수점",
                LocalDate.of(2026, 3, 14),
                5,
                "겉은 바삭하고 속이 촉촉해서 만족했어요",
                LocalDateTime.of(2026, 3, 14, 9, 30),
                LocalDateTime.of(2026, 3, 14, 10, 15)
        );
    }

    private ObjectMapper snakeCaseObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }
}
