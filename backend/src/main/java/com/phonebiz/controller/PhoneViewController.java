package com.phonebiz.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.PhoneViewDTO;
import com.phonebiz.service.PhoneViewService;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/phone-views")
@RequiredArgsConstructor
public class PhoneViewController {

    private final PhoneViewService phoneViewService;

    @GetMapping
    @PreAuthorize("hasAuthority('phone:view') or hasRole('ADMIN')")
    public ApiResponse<Page<PhoneViewDTO>> listPhones(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(phoneViewService.listPhones(keyword, status, orgId, pageable));
    }
}
