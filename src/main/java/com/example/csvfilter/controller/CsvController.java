package com.example.csvfilter.controller;

import com.example.csvfilter.model.UserSessionData;
import com.example.csvfilter.parser.exception.FilterException;
import com.example.csvfilter.service.DataService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Map;

@Controller
public class CsvController {

    private final DataService dataService;
    private final UserSessionData userSessionData;

    public CsvController(DataService dataService, UserSessionData userSessionData) {
        this.dataService = dataService;
        this.userSessionData = userSessionData;
    }

    @GetMapping("/")
    public String index() {
        if (userSessionData.hasData()) {
            return "redirect:/view";
        }
        return "index";
    }

    @PostMapping("/upload")
    public String uploadCsv(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a CSV file to upload.");
            return "redirect:/";
        }
        try {
            dataService.loadAndStoreCsv(file.getInputStream());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to process file: " + e.getMessage());
            return "redirect:/";
        }
        return "redirect:/view";
    }

    @GetMapping("/view")
    public String viewData(
            @RequestParam(required = false, defaultValue = "") String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        if (!userSessionData.hasData()) {
            return "redirect:/";
        }

        model.addAttribute("headers", userSessionData.getHeaders());
        model.addAttribute("currentFilter", filter);
        model.addAttribute("columnNames", userSessionData.getColumnNames());

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Map<String, Object>> paginatedData = dataService.getFilteredPaginatedData(filter, pageable);
            model.addAttribute("page", paginatedData);
        } catch (FilterException e) {
            model.addAttribute("error", e.getMessage());
            // Show empty page on error
            model.addAttribute("page", Page.empty());
        }

        return "view";
    }

    @GetMapping("/export")
    public void exportData(
            @RequestParam(required = false, defaultValue = "") String filter,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        if (!userSessionData.hasData()) {
            // Cannot export, redirect
            return;
        }

        try {
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; file=\"filtered_data.csv\"");
            dataService.exportFilteredData(filter, response.getWriter());
        } catch (FilterException e) {
            // How to handle error in a download? Redirect with flash attribute.
            redirectAttributes.addFlashAttribute("error", "Export failed: " + e.getMessage());
        } catch (IOException e) {
            // Handle IO exception
        }
    }
}