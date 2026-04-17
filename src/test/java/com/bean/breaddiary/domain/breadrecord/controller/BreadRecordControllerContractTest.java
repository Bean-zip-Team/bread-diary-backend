package com.bean.breaddiary.domain.breadrecord.controller;

import com.bean.breaddiary.domain.bread.entity.BreadType;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.request.CreateNewBreadRecordRequest;
import com.bean.breaddiary.domain.breadrecord.dto.response.BreadRecordCreateResponse;
import com.bean.breaddiary.domain.breadrecord.service.BreadRecordComplexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BreadRecordControllerContractTest {

    private static final UUID DUMMY_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private BreadRecordComplexService breadRecordComplexService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        breadRecordComplexService = mock(BreadRecordComplexService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BreadRecordController(breadRecordComplexService))
                .build();
    }

    @Test
    void createBreadRecordBindsSnakeCaseMultipartFieldsAndSerializesSnakeCaseResponse() throws Exception {
        UUID breadId = UUID.fromString("b0e1f2a3-c4d5-6789-abcd-ef0123456789");
        BreadRecordCreateResponse response = createResponse(breadId, BreadType.PASTRY, false);

        when(breadRecordComplexService.createBreadRecord(eq(DUMMY_USER_ID), any(CreateBreadRecordRequest.class)))
                .thenReturn(response);

        mockMvc.perform(multipart("/breads")
                        .file(new MockMultipartFile(
                                "photo",
                                "croissant.webp",
                                MediaType.IMAGE_JPEG_VALUE,
                                "photo".getBytes()
                        ))
                        .param("bread_id", breadId.toString())
                        .param("shop_name", "르뺑블루 성수점")
                        .param("eaten_date", "2026-03-14")
                        .param("rating", "5")
                        .param("review", "겉은 바삭하고 안은 촉촉해서 완벽했어요."))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bread_id").value(breadId.toString()))
                .andExpect(jsonPath("$.data.sticker_number").value(7))
                .andExpect(jsonPath("$.data.photo_url").value("https://cdn.bread-diary.app/bread-photos/record.webp"))
                .andExpect(jsonPath("$.data.photo_thumbnail_url").value("https://cdn.bread-diary.app/bread-photos/record_thumb.webp"))
                .andExpect(jsonPath("$.data.bread_type").value("PASTRY"))
                .andExpect(jsonPath("$.data.shop_name").value("르뺑블루 성수점"))
                .andExpect(jsonPath("$.data.eaten_date").value("2026-03-14"))
                .andExpect(jsonPath("$.data.is_first_record").value(false))
                .andExpect(jsonPath("$.data.breadId").doesNotExist())
                .andExpect(jsonPath("$.data.shopName").doesNotExist());

        ArgumentCaptor<CreateBreadRecordRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateBreadRecordRequest.class);
        verify(breadRecordComplexService).createBreadRecord(eq(DUMMY_USER_ID), requestCaptor.capture());

        CreateBreadRecordRequest request = requestCaptor.getValue();
        assertEquals(breadId, request.getBreadId());
        assertEquals("르뺑블루 성수점", request.getShopName());
        assertEquals(LocalDate.of(2026, 3, 14), request.getEatenDate());
    }

    @Test
    void createNewBreadRecordBindsSnakeCaseMultipartFields() throws Exception {
        UUID breadId = UUID.fromString("c1d2e3f4-a5b6-7890-cdef-ab0123456789");
        BreadRecordCreateResponse response = createResponse(breadId, BreadType.BAGEL, true);

        when(breadRecordComplexService.createNewBreadRecord(eq(DUMMY_USER_ID), any(CreateNewBreadRecordRequest.class)))
                .thenReturn(response);

        mockMvc.perform(multipart("/breads/new")
                        .file(new MockMultipartFile(
                                "photo",
                                "bagel.webp",
                                MediaType.IMAGE_JPEG_VALUE,
                                "photo".getBytes()
                        ))
                        .param("name", "플레인 베이글")
                        .param("bread_type", "BAGEL")
                        .param("shop_name", "베이글샵")
                        .param("eaten_date", "2026-03-15")
                        .param("rating", "4")
                        .param("review", "쫄깃했어요."))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bread_id").value(breadId.toString()))
                .andExpect(jsonPath("$.data.bread_type").value("BAGEL"))
                .andExpect(jsonPath("$.data.is_first_record").value(true))
                .andExpect(jsonPath("$.data.breadType").doesNotExist());

        ArgumentCaptor<CreateNewBreadRecordRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateNewBreadRecordRequest.class);
        verify(breadRecordComplexService).createNewBreadRecord(eq(DUMMY_USER_ID), requestCaptor.capture());

        CreateNewBreadRecordRequest request = requestCaptor.getValue();
        assertEquals("플레인 베이글", request.getName());
        assertEquals(BreadType.BAGEL, request.getBreadType());
        assertEquals("베이글샵", request.getShopName());
        assertEquals(LocalDate.of(2026, 3, 15), request.getEatenDate());
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
                "르뺑블루 성수점",
                LocalDate.of(2026, 3, 14),
                5,
                "겉은 바삭하고 안은 촉촉해서 완벽했어요.",
                isFirstRecord,
                LocalDateTime.of(2026, 3, 14, 9, 30)
        );
    }
}
