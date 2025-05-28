package com.bank.ctrl;

import com.bank.dto.response.CustomerStatsResponseDto;
import com.bank.sv.CustomerStatsService;
import com.bank.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/customer-stats")
@Tag(name = "Customer Stats")
public class CustomerStatsController {

    @Autowired
    private APIResponse apiResponse;

    @Autowired
    private CustomerStatsService customerStatsService;

    @Operation(summary = "Generate customers stats for a specific month ")
    @PostMapping("/generate")
    public ResponseEntity<Object> generateStats(
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {

        customerStatsService.generateStatsForMonth(year, month);
        return ResponseEntity.ok(apiResponse.response("Customer stats generated successfully", true, null));
    }

    @Operation(summary = "Get customer stats in a date range")
    @GetMapping("/customer")
    public ResponseEntity<Object> getStatsForCustomer(
            @RequestParam("customerId") String customerId,
            @RequestParam("startDate") @DateTimeFormat(pattern = "dd-MM-yyyy") Date startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "dd-MM-yyyy") Date endDate) {

        List<CustomerStatsResponseDto> stats = customerStatsService.getStatsForCustomer(customerId, startDate, endDate);
        return ResponseEntity.ok(apiResponse.response("Retrieved data successfully", true, stats));
    }

    @Operation(summary = "Export customer statistics to Excel")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportStats(
            @RequestParam Integer year,
            @RequestParam(required = false) Integer month) {

        byte[] excelFile = customerStatsService.exportToExcel(year, month);
        String filename = month != null ? String.format("customer_stats_%d_%02d.xlsx", year, month)
                : String.format("customer_stats_%d.xlsx", year);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelFile);

    }

    @Operation(summary = "Export customer statistics by weekly to Excel")
    @GetMapping("/export/daily/month/{month}")
    public ResponseEntity<byte[]> exportStatsWeekly(
            @PathVariable Integer month,
            @RequestParam String customerId) {

        byte[] excelFile = customerStatsService.exportWeeklyStatsToExcel(customerId, month);
        String filename = String.format("customer_stats_%s_%02d.xlsx", customerId, month);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelFile);
    }

    @Operation(summary = "Export customer statistics by quarter to Excel")
    @GetMapping("/export/quarter/{year}")
    public ResponseEntity<byte[]> exportStatsQuarter(
            @PathVariable Integer year,
            @RequestParam String customerId) {
        byte[] excelFile = customerStatsService.exportQuarterlyStatsToExcel(customerId, year);
        String filename = String.format("customer_stats_%s_%02d.xlsx", customerId, year);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelFile);
    }

    @Operation(summary = "Export customer statistics by year to Excel")
    @GetMapping("/export/daily/{year}")
    public ResponseEntity<byte[]> exportStatsYearly(
            @PathVariable Integer year,
            @RequestParam String customerId) {
        byte[] excelFile = customerStatsService.exportYearlyStatsToExcel(customerId, year);
        String filename = String.format("customer_stats_%s_%02d.xlsx", customerId, year);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelFile);
    }
}
