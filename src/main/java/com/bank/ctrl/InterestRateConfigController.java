package com.bank.ctrl;

import com.bank.dto.request.InterestRateRequestDto;
import com.bank.model.InterestRateConfig;
import com.bank.sv.InterestRateConfigService;
import com.bank.utils.APIResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interest-rates")
@Tag(name = "Interest Rates")
public class InterestRateConfigController {
    @Autowired
    private InterestRateConfigService interestRateConfigService;

    @Autowired
    private APIResponse apiResponse;

    @GetMapping
    public ResponseEntity<Object> getAllRates(){
        try{
            List<InterestRateConfig> rates = interestRateConfigService.getAllActiveRates();
            return ResponseEntity.ok(apiResponse.response("Retrieved successfully", true, rates));
        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(apiResponse.response("Rates not found", false, null));
        }
    }

    @PostMapping
    public ResponseEntity<Object> createRate(@Valid @RequestBody(required = true)InterestRateRequestDto requestDto){
        try {
            interestRateConfigService.createRate(requestDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(apiResponse.response(e.getMessage(), false, null));
        }
        return ResponseEntity.ok(apiResponse.response("Create successfully", true, null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> updateRate(@PathVariable String id, @Valid @RequestBody(required = true)InterestRateRequestDto requestDto){
        try {
            interestRateConfigService.updateRate(id, requestDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(apiResponse.response(e.getMessage(), false, null));
        }
        return ResponseEntity.ok(apiResponse.response("Update successfully", true, null));
    }

    @PutMapping("/status/{id}")
    public ResponseEntity<Object> updateRateStatus(@PathVariable String id) {
        try {
            interestRateConfigService.updateRateStatus(id);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(apiResponse.response(e.getMessage(), false, null));
        }
        return ResponseEntity.ok(apiResponse.response("Update status successfully", true, null));
    }
}
