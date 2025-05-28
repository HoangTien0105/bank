package com.bank.ctrl;

import com.bank.dto.PaginDto;
import com.bank.dto.request.AlertUpdateRequest;
import com.bank.dto.response.AlertResponseDto;
import com.bank.model.JwtUser;
import com.bank.sv.AlertService;
import com.bank.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alerts")
public class AlertController {
    @Autowired
    private APIResponse apiResponse;

    @Autowired
    private AlertService alertService;

    @GetMapping
    public ResponseEntity<Object> getAlerts(@RequestParam(value = "offset", defaultValue = "0") Integer offset,
                                            @RequestParam(value = "limit", defaultValue = "10") Integer limit,
                                            @RequestParam(value = "keyword", required = false) String keyword,
                                            @RequestParam(value = "type", required = false) String type,
                                            @RequestParam(value = "status", required = false) String status) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();

        PaginDto<AlertResponseDto> paginDto = new PaginDto<>();
        paginDto.setOffset(offset);
        paginDto.setLimit(limit);
        paginDto.setKeyword(keyword);

        Map<String, Object> options = new HashMap<>();
        if (type != null) {
            options.put("type", type);
        }
        if (status != null) {
            options.put("status", status);
        }
        paginDto.setOptions(options);

        PaginDto<AlertResponseDto> result = alertService.getAlerts(paginDto, jwtUser.getRole());

        return ResponseEntity.ok(apiResponse.response("Retrieved alert successfully", true, result));
    }

    @GetMapping("{id}")
    public ResponseEntity<Object> getAlertById(@PathVariable(value = "id") String id) {
        AlertResponseDto response = alertService.getAlertById(id);
        return ResponseEntity.ok(apiResponse.response("Retrieved alert successfully", true, response));
    }

    @Operation(summary = "Update alert status")
    @PutMapping("{id}/status")
    public ResponseEntity<Object> updateAlertStatus(@PathVariable(value = "id") String id,
                                                    @Valid @RequestBody AlertUpdateRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();

        alertService.updateAlertStatus(id, request.getStatus(), jwtUser.getId(), request.getNotes());
        return ResponseEntity.ok(apiResponse.response("Update alert status successfully", true, null));
    }

    @Operation(summary = "Detect unusual transaction")
    @PostMapping
    public ResponseEntity<Object> detectAbnormalTransactions() {
        alertService.detectAbnormalTransactions();
        return ResponseEntity.ok(apiResponse.response("Complete detection successfully", true, null));
    }
}
