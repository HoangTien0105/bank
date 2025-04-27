package com.bank.dto;

import java.util.*;

public class PaginDto<T> {

    private Integer offset;

    private Integer limit;

    private List<T> results = new ArrayList<>();

    private Integer pageNumber;

    private Long totalRows;

    private Integer totalPages;

    private String keyword;

    private Map<String, Object> options = new LinkedHashMap<>();

    public Long getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Long totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getOffset(){
        if (offset == null || offset < 0){
            offset = 0;
        }
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public List<T> getResults() {
        return results;
    }

    public void setResults(List<T> results) {
        this.results = results;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Map<String, Object> getOptions() {

        if (options == null) {
            options = new LinkedHashMap<>();
        }
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    public void setOffset(String offset) {
        try {
            this.offset = Integer.parseInt(offset);
        } catch (Exception e) {
            this.offset = 0;
        }
    }

    public void setLimit(String limit) {
        try {
            this.limit = Integer.parseInt(limit);
        } catch (Exception e) {
            this.limit = 10;
        }
    }

}
