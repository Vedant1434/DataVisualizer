package com.example.csvfilter.util;

import com.example.csvfilter.parser.exception.FilterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(MaxUploadSizeExceededException exc, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "File upload failed: File is too large. Max size is 50MB.");
        return "redirect:/";
    }

    @ExceptionHandler(FilterException.class)
    public String handleFilterException(FilterException exc, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Filter Error: " + exc.getMessage());
        return "redirect:/view";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception exc, RedirectAttributes redirectAttributes) {
        // Catch-all for other unexpected errors
        redirectAttributes.addFlashAttribute("error", "An unexpected error occurred: " + exc.getMessage());
        return "redirect:/";
    }
}