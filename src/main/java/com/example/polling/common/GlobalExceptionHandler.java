package com.example.polling.common;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ForbiddenException.class)
    public String forbidden(ForbiddenException ex, Model model) {
        model.addAttribute("title", "Access denied");
        model.addAttribute("message", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(NotFoundException.class)
    public String notFound(NotFoundException ex, Model model) {
        model.addAttribute("title", "Not found");
        model.addAttribute("message", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(AppException.class)
    public String appError(AppException ex, Model model) {
        model.addAttribute("title", "Action needed");
        model.addAttribute("message", ex.getMessage());
        return "error";
    }
}
