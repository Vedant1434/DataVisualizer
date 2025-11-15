package com.example.csvfilter.controller;

import com.example.csvfilter.model.UserSessionData;
import com.example.csvfilter.parser.exception.FilterException;
import com.example.csvfilter.service.DataService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort; // <-- IMPORT
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
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
            if (userSessionData.getHeaders() == null || userSessionData.getSchema() == null) {
                userSessionData.clearData();
                return "index";
            }
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

    // --- METHOD MODIFIED to handle Sort ---
    @GetMapping("/view")
    public String viewData(
            @RequestParam(required = false, defaultValue = "") String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> cols,
            @RequestParam(required = false, defaultValue = "") String sort, // <-- NEW
            @RequestParam(required = false, defaultValue = "ASC") String dir, // <-- NEW
            Model model) {

        if (!userSessionData.hasData()) {
            return "redirect:/";
        }

        List<String> allHeaders = userSessionData.getHeaders();
        if (allHeaders == null || userSessionData.getSchema() == null) {
            userSessionData.clearData();
            return "redirect:/";
        }
        List<String> selectedHeaders = cols;
        if (selectedHeaders == null || selectedHeaders.isEmpty()) {
            selectedHeaders = allHeaders;
        }

        model.addAttribute("allHeaders", allHeaders);
        model.addAttribute("selectedHeaders", selectedHeaders);
        model.addAttribute("currentFilter", filter);

        // --- NEW SORTING LOGIC ---
        Sort sortOrder = Sort.unsorted();
        if (sort != null && !sort.isBlank() && allHeaders.contains(sort)) {
            sortOrder = Sort.by(Sort.Direction.fromString(dir), sort);
        }

        model.addAttribute("currentSort", sort);
        model.addAttribute("currentDir", dir);
        // --- END NEW SORTING LOGIC ---

        try {
            // Pageable now includes the sort order
            Pageable pageable = PageRequest.of(page, size, sortOrder);
            Page<Map<String, Object>> paginatedData = dataService.getFilteredPaginatedData(filter, pageable);
            model.addAttribute("page", paginatedData);
        } catch (FilterException e) {
            model.addAttribute("error", e.getMessage());
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
            return;
        }

        try {
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=\"filtered_data.csv\"");
            dataService.exportFilteredData(filter, response.getWriter());
        } catch (FilterException e) {
            redirectAttributes.addFlashAttribute("error", "Export failed: " + e.getMessage());
        } catch (IOException e) {
            // Handle IO exception
        }
    }

    @GetMapping("/new")
    public String startNew(SessionStatus sessionStatus, HttpSession session) {
        userSessionData.clearData();
        sessionStatus.setComplete();
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/?cleared=true";
    }
}