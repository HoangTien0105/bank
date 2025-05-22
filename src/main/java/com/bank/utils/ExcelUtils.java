package com.bank.utils;

import com.bank.dto.response.CustomerStatsResponseDto;
import com.bank.model.AdminStatistics;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExcelUtils {
    public static byte[] createCustomerStatsExcel(List<CustomerStatsResponseDto> statsList, Integer year, Integer month) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()){
            //Tạo sheet dữ liệu
            XSSFSheet dataSheet = workbook.createSheet("Customer statistics");

            String[] headers = {
                    "ID", "Customer", "Year", "Month", "Total Transactions", "Highest Transaction", "Average Transaction", "Lowest Transaction",
                    "Month-End Balance", "Total Transaction Amount"
            };

            Row headerRow = dataSheet.createRow(2);
            CellStyle headerStyle = createHeaderStyle(workbook);

            for(int i = 0; i < headers.length; i++){
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            //Điền dữ liệu
            int rowNum = 3;
            for(CustomerStatsResponseDto stats: statsList){
                Row row = dataSheet.createRow(rowNum++);

                row.createCell(0).setCellValue(stats.getId());
                row.createCell(1).setCellValue(stats.getCustomer());
                row.createCell(2).setCellValue(stats.getYear());
                row.createCell(3).setCellValue(stats.getMonth());
                row.createCell(4).setCellValue(stats.getTotalTransactions());
                row.createCell(5).setCellValue(stats.getMaxTransactionAmount().doubleValue());
                row.createCell(6).setCellValue(stats.getAvgTransactionAmount().doubleValue());
                row.createCell(7).setCellValue(stats.getMinTransactionAmount().doubleValue());
                row.createCell(8).setCellValue(stats.getEndMonthBalance().doubleValue());
                row.createCell(9).setCellValue(stats.getTotalTransactionAmount().doubleValue());
            }

            // auto size
            for(int i = 0; i < headers.length; i++) {
                dataSheet.autoSizeColumn(i);
            }

            // Chart sheet
            if (!statsList.isEmpty()) {
                createCustomerStatsCharts(workbook, statsList);
            }

            //Ghi vào ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static byte[] createAdminStatsExcel(List<AdminStatistics> statsList, String period) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // sheet
            XSSFSheet dataSheet = workbook.createSheet("Admin statistics");

            // header
            String[] headers = {
                    "Day", "Total Transactions", "Highest Transaction", "Average Transaction", "Lowest Transaction",
                    "New Customers", "Total Customers", "New Saving Accounts"
            };

            Row headerRow = dataSheet.createRow(2);
            CellStyle headerStyle = createHeaderStyle(workbook);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // fill data
            int rowNum = 3;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (AdminStatistics stats : statsList) {
                Row row = dataSheet.createRow(rowNum++);

                row.createCell(0).setCellValue(stats.getDate().format(formatter));
                row.createCell(1).setCellValue(stats.getTotalTransactions());

                if (stats.getMaxTransactionAmount() != null) {
                    row.createCell(2).setCellValue(stats.getMaxTransactionAmount().doubleValue());
                } else {
                    row.createCell(2).setCellValue(0);
                }

                if (stats.getAvgTransactionAmount() != null) {
                    row.createCell(3).setCellValue(stats.getAvgTransactionAmount().doubleValue());
                } else {
                    row.createCell(3).setCellValue(0);
                }

                if (stats.getMinTransactionAmount() != null) {
                    row.createCell(4).setCellValue(stats.getMinTransactionAmount().doubleValue());
                } else {
                    row.createCell(4).setCellValue(0);
                }

                row.createCell(5).setCellValue(stats.getNewCustomers());
                row.createCell(6).setCellValue(stats.getTotalCustomers());
                row.createCell(7).setCellValue(stats.getNewSavingAccounts());
            }

            // Auto size
            for (int i = 0; i < headers.length; i++) {
                dataSheet.autoSizeColumn(i);
            }

            // sheet chart
            if (!statsList.isEmpty()) {
                createAdminStatsCharts(workbook, statsList);
            }

            // Ghi vào ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private static void createCustomerStatsCharts(XSSFWorkbook workbook, List<CustomerStatsResponseDto> statsList) {
        XSSFSheet chartSheet = workbook.createSheet("Chart");

        // 1. Biểu đồ xu hướng giao dịch
        createTransactionTrendChart(workbook, chartSheet, statsList, 0);

        // 2. Biểu đồ số dư cuối tháng
        createBalanceTrendChart(workbook, chartSheet, statsList, 15);

        // 3. Biểu đồ giá trị giao dịch
        createTransactionValueChart(workbook, chartSheet, statsList, 30);
    }

    private static void createAdminStatsCharts(XSSFWorkbook workbook, List<AdminStatistics> statsList) {
        XSSFSheet chartSheet = workbook.createSheet("Chart");

        // 1. Biểu đồ xu hướng giao dịch
        createAdminTransactionTrendChart(workbook, chartSheet, statsList, 0);

        // 2. Biểu đồ khách hàng mới và tổng khách hàng
        createCustomerTrendChart(workbook, chartSheet, statsList, 15);

        // 3. Biểu đồ giá trị giao dịch
        createAdminTransactionValueChart(workbook, chartSheet, statsList, 30);
    }

    /// Biểu đồ xu hướng số lượng giao dịch
    private static void createTransactionTrendChart(XSSFWorkbook workbook, XSSFSheet sheet, List<CustomerStatsResponseDto> statsList, Integer startRow){
        Row titleRow = sheet.createRow(startRow);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Transaction Volume Trend Chart");

        //Chart data
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, startRow + 2, 10, startRow + 12);

        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("Transaction Volume Trend");
        chart.setTitleOverlay(false);

        //Tạo trục X Y
        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle("Months");
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("Num of transactions");

        //Data
        XDDFDataSource<String> months = XDDFDataSourcesFactory.fromArray(
                statsList.stream()
                        .map(s -> s.getMonth() + "/" + s.getYear())
                        .toArray(String[]::new));

        XDDFNumericalDataSource<Double> transactions = XDDFDataSourcesFactory.fromArray(
                statsList.stream()
                        .map(s -> s.getTotalTransactions().doubleValue())
                        .toArray(Double[]::new));

        //Series
        XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);
        XDDFLineChartData.Series series = (XDDFLineChartData.Series) data.addSeries(months, transactions);
        series.setTitle("Number of transactions", null);
        series.setMarkerStyle(MarkerStyle.CIRCLE);

        //Vẽ chart
        chart.plot(data);
    }

    private static void createBalanceTrendChart(XSSFWorkbook workbook, XSSFSheet sheet,
                                                List<CustomerStatsResponseDto> statsList, int startRow) {
        Row titleRow = sheet.createRow(startRow);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("End-Month Balance Trend Chart");

        //Chart data
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, startRow + 2, 10, startRow + 12);

        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("End-Month Balance Trend");
        chart.setTitleOverlay(false);

        // Tạo trục X Y
        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle("Months");
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("Balance (VND)");

        // Tạo dữ liệu
        XDDFDataSource<String> months = XDDFDataSourcesFactory.fromArray(
                statsList.stream()
                        .map(s -> s.getMonth() + "/" + s.getYear())
                        .toArray(String[]::new));

        XDDFNumericalDataSource<Double> balances = XDDFDataSourcesFactory.fromArray(
                statsList.stream()
                        .map(s -> s.getEndMonthBalance().doubleValue())
                        .toArray(Double[]::new));

        // Tạo series
        XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);
        XDDFLineChartData.Series series = (XDDFLineChartData.Series) data.addSeries(months, balances);
        series.setTitle("End-Month Balance tháng", null);
        series.setMarkerStyle(MarkerStyle.CIRCLE);

        // Vẽ
        chart.plot(data);
    }

    private static void createTransactionValueChart(XSSFWorkbook workbook, XSSFSheet sheet,
                                                    List<CustomerStatsResponseDto> statsList, int startRow) {
        Row titleRow = sheet.createRow(startRow);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Transaction Value Chart");

        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, startRow + 2, 10, startRow + 12);

        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("Transaction Value Base On Months");
        chart.setTitleOverlay(false);

        // Tạo trục X Y
        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle("Months");
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("Amount (VND)");

        //data
        XDDFDataSource<String> months = XDDFDataSourcesFactory.fromArray(
                statsList.stream()
                        .map(s -> s.getMonth() + "/" + s.getYear())
                        .toArray(String[]::new));

        XDDFNumericalDataSource<Double> maxValues = XDDFDataSourcesFactory.fromArray(
                statsList.stream()
                        .map(s -> s.getMaxTransactionAmount().doubleValue())
                        .toArray(Double[]::new));

        XDDFNumericalDataSource<Double> avgValues = XDDFDataSourcesFactory.fromArray(
                statsList.stream()
                        .map(s -> s.getAvgTransactionAmount().doubleValue())
                        .toArray(Double[]::new));

        XDDFNumericalDataSource<Double> minValues = XDDFDataSourcesFactory.fromArray(
                statsList.stream()
                        .map(s -> s.getMinTransactionAmount().doubleValue())
                        .toArray(Double[]::new));

        // series
        XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);

        XDDFLineChartData.Series maxSeries = (XDDFLineChartData.Series) data.addSeries(months, maxValues);
        maxSeries.setTitle("Highest amount", null);
        maxSeries.setMarkerStyle(MarkerStyle.CIRCLE);

        XDDFLineChartData.Series avgSeries = (XDDFLineChartData.Series) data.addSeries(months, avgValues);
        avgSeries.setTitle("Average amount", null);
        avgSeries.setMarkerStyle(MarkerStyle.CIRCLE);

        XDDFLineChartData.Series minSeries = (XDDFLineChartData.Series) data.addSeries(months, minValues);
        minSeries.setTitle("Lowest amount", null);
        minSeries.setMarkerStyle(MarkerStyle.CIRCLE);

        // Vẽ
        chart.plot(data);

        // Thêm legend
        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.TOP_RIGHT);
    }

    /// Admin
    private static void createAdminTransactionTrendChart(XSSFWorkbook workbook, XSSFSheet sheet,
                                                         List<AdminStatistics> statsList, int startRow) {
        Row titleRow = sheet.createRow(startRow);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Transaction Volume Trend Chart");

        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, startRow + 2, 10, startRow + 12);

        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("Transaction Volume Trend");
        chart.setTitleOverlay(false);

        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle("Date");
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("Num of transactions");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        XDDFDataSource<String> dates = XDDFDataSourcesFactory.fromArray(
                statsList.stream()
                        .map(s -> s.getDate().format(formatter))
                        .toArray(String[]::new));

        XDDFNumericalDataSource<Double> transactions = XDDFDataSourcesFactory.fromArray(
                statsList.stream()
                        .map(s -> s.getTotalTransactions().doubleValue())
                        .toArray(Double[]::new));

        XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);
        XDDFLineChartData.Series series = (XDDFLineChartData.Series) data.addSeries(dates, transactions);
        series.setTitle("Transactions amount", null);
        series.setMarkerStyle(MarkerStyle.CIRCLE);

        chart.plot(data);
    }

    /// xu hướng customer
    private static void createCustomerTrendChart(XSSFWorkbook workbook, XSSFSheet sheet,
                                                 List<AdminStatistics> statsList, int startRow) {
        Row titleRow = sheet.createRow(startRow);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Customer Trend Chart");

        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, startRow + 2, 10, startRow + 12);

        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("Customer Trend");
        chart.setTitleOverlay(false);

        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle("Date");
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("Num of customers");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        XDDFDataSource<String> dates = XDDFDataSourcesFactory.fromArray(
                statsList.stream()
                        .map(s -> s.getDate().format(formatter))
                        .toArray(String[]::new));

        XDDFNumericalDataSource<Double> newCustomers = XDDFDataSourcesFactory.fromArray(
                statsList.stream()
                        .map(s -> s.getNewCustomers().doubleValue())
                        .toArray(Double[]::new));

        XDDFNumericalDataSource<Double> totalCustomers = XDDFDataSourcesFactory.fromArray(
                statsList.stream()
                        .map(s -> s.getTotalCustomers().doubleValue())
                        .toArray(Double[]::new));

        XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);

        XDDFLineChartData.Series newSeries = (XDDFLineChartData.Series) data.addSeries(dates, newCustomers);
        newSeries.setTitle("New customers", null);
        newSeries.setMarkerStyle(MarkerStyle.CIRCLE);

        XDDFLineChartData.Series totalSeries = (XDDFLineChartData.Series) data.addSeries(dates, totalCustomers);
        totalSeries.setTitle("Total customers", null);
        totalSeries.setMarkerStyle(MarkerStyle.CIRCLE);

        chart.plot(data);

        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.TOP_RIGHT);
    }

    private static void createAdminTransactionValueChart(XSSFWorkbook workbook, XSSFSheet sheet,
                                                         List<AdminStatistics> statsList, int startRow) {
        Row titleRow = sheet.createRow(startRow);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Amount transaction chart");

        // Tạo dữ liệu cho biểu đồ
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, startRow + 2, 10, startRow + 12);

        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("Amount/date ");
        chart.setTitleOverlay(false);

        // Tạo trục X và Y
        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle("Date");
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("Amount (VND)");

        // Tạo dữ liệu
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        XDDFDataSource<String> dates = XDDFDataSourcesFactory.fromArray(
                statsList.stream()
                        .map(s -> s.getDate().format(formatter))
                        .toArray(String[]::new));

        XDDFNumericalDataSource<Double> maxValues = XDDFDataSourcesFactory.fromArray(
                statsList.stream()
                        .map(s -> s.getMaxTransactionAmount() != null ? s.getMaxTransactionAmount().doubleValue() : 0.0)
                        .toArray(Double[]::new));

        XDDFNumericalDataSource<Double> avgValues = XDDFDataSourcesFactory.fromArray(
                statsList.stream()
                        .map(s -> s.getAvgTransactionAmount() != null ? s.getAvgTransactionAmount().doubleValue() : 0.0)
                        .toArray(Double[]::new));

        XDDFNumericalDataSource<Double> minValues = XDDFDataSourcesFactory.fromArray(
                statsList.stream()
                        .map(s -> s.getMinTransactionAmount() != null ? s.getMinTransactionAmount().doubleValue() : 0.0)
                        .toArray(Double[]::new));

        // Tạo line chart data
        XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);

        // Tạo series cho giá trị cao nhất
        XDDFLineChartData.Series maxSeries = (XDDFLineChartData.Series) data.addSeries(dates, maxValues);
        maxSeries.setTitle("Maximum transaction", null);
        maxSeries.setMarkerStyle(MarkerStyle.CIRCLE);

        // Tạo series cho giá trị trung bình
        XDDFLineChartData.Series avgSeries = (XDDFLineChartData.Series) data.addSeries(dates, avgValues);
        avgSeries.setTitle("Average transaction", null);
        avgSeries.setMarkerStyle(MarkerStyle.CIRCLE);

        // Tạo series cho giá trị thấp nhất
        XDDFLineChartData.Series minSeries = (XDDFLineChartData.Series) data.addSeries(dates, minValues);
        minSeries.setTitle("Min transaction", null);
        minSeries.setMarkerStyle(MarkerStyle.CIRCLE);

        // Vẽ biểu đồ
        chart.plot(data);

        // Thêm legend
        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.TOP_RIGHT);
    }

    /// Create cell styles for header
    public static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);

        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        return headerStyle;
    }

    /// Create a cell style for cells
    public static CellStyle createDataStyle(Workbook workbook) {
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        return dataStyle;
    }
}
