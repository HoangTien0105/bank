package com.bank.ctrl;

import com.bank.model.AdminStatistics;
import com.bank.sv.AdminStatsService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/admin-stats")
@Tag(name = "Admin Stats")
public class AdminStatsController {
    @Autowired
    private APIResponse apiResponse;

    @Autowired
    private AdminStatsService adminStatsService;

    @Operation(summary = "Generate admin statistics for a specific date")
    @PostMapping("/generate")
    public ResponseEntity<Object> generateStatistics(
            @RequestParam("date") @DateTimeFormat(pattern = "dd-MM-yyyy") Date date) {
        adminStatsService.generateStatisticsForDate(date);
        return ResponseEntity.ok(apiResponse.response("Admin statistics generated successfully", true, null));
    }

    @Operation(summary = "Get statistics for a date range")
    @GetMapping("/range")
    public ResponseEntity<Object> getStatisticsForRange(
            @RequestParam("startDate") @DateTimeFormat(pattern = "dd-MM-yyyy") Date startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "dd-MM-yyyy") Date endDate) {
        List<AdminStatistics> adminStatistics = adminStatsService.getStatisticsForDateRange(startDate, endDate);
        return ResponseEntity.ok(apiResponse.response("Admin statistics retrieved successfully", true, adminStatistics));
    }

    @Operation(summary = "Get statistics for a specific date")
    @GetMapping("/date")
    public ResponseEntity<Object> getStatisticsForDate(
            @RequestParam("date") @DateTimeFormat(pattern = "dd-MM-yyyy") Date date) {
        AdminStatistics adminStatistics = adminStatsService.getStatisticsForDate(date);
        return ResponseEntity.ok(apiResponse.response("Admin statistics retrieved successfully", true, adminStatistics));
    }

    @Operation(summary = "Get statistics for a week")
    @GetMapping("/week")
    public ResponseEntity<Object> getStatisticsForWeek(
            @RequestParam("date") @DateTimeFormat(pattern = "dd-MM-yyyy") Date date) {
        Map<String, Object> weeklyStats = adminStatsService.getStatisticsForWeek(date);
        return ResponseEntity.ok(apiResponse.response("Weekly statistics retrieved successfully", true, weeklyStats));
    }

    @Operation(summary = "Get statistics for a quarter")
    @GetMapping("/quarter")
    public ResponseEntity<Object> getStatisticsForQuarter(
            @RequestParam("quarter") int quarter,
            @RequestParam("year") int year) {

        Map<String, Object> quarterlyStats = adminStatsService.getStatisticsForQuarter(quarter, year);
        return ResponseEntity.ok(apiResponse.response("Quarterly statistics retrieved successfully", true, quarterlyStats));
    }

    @Operation(summary = "Get statistics for a year")
    @GetMapping("/year")
    public ResponseEntity<Object> getStatisticsForYear(
            @RequestParam("year") int year) {
        Map<String, Object> yearlyStats = adminStatsService.getStatisticsForYear(year);
        return ResponseEntity.ok(apiResponse.response("Yearly statistics retrieved successfully", true, yearlyStats));
    }

    @Operation(summary = "Export daily statistics to Excel")
    @GetMapping("/export/daily")
    public ResponseEntity<byte[]> exportDailyStats(
            @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") Date endDate) {
        byte[] excelFile = adminStatsService.exportDailyStatsToExcel(startDate, endDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=daily_stats.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelFile);
    }

    @Operation(summary = "Export weekly statistics to Excel")
    @GetMapping("/export/weekly/{year}")
    public ResponseEntity<byte[]> exportWeeklyStats(@PathVariable int year) {
        byte[] excelFile = adminStatsService.exportWeeklyStatsToExcel(year);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=weekly_stats.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelFile);
    }

    @Operation(summary = "Export quarterly statistics to Excel")
    @GetMapping("/export/quarterly/{year}")
    public ResponseEntity<byte[]> exportQuarterlyStats(@PathVariable int year) {
        byte[] excelFile = adminStatsService.exportQuarterlyStatsToExcel(year);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=quarterly_stats.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelFile);
    }

    @Operation(summary = "Export yearly statistics to Excel")
    @GetMapping("/export/yearly")
    public ResponseEntity<byte[]> exportYearlyStats(
            @RequestParam int year) {
        byte[] excelFile = adminStatsService.exportYearlyStatsToExcel(year);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=yearly_stats.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelFile);

    }

    @Operation(summary = "Get total stats")
    @GetMapping("/total")
    public ResponseEntity<Object> getTotalAdminStats() {
        Map<String, Object> response = adminStatsService.getTotalAdminStatForCurrentDate();
        return ResponseEntity.ok().body(apiResponse.response("Retrieved data successfully", true, response));
    }
}
