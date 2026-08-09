package com.internetprog.shopex.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Unknown URLs fall through to the static-resource handler, which throws
     * this. Without a dedicated handler the catch-all below would turn every
     * mistyped URL into a 500.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleMissingResource() {
        return "error/404";
    }

    /**
     * Controllers/services signal "not found" / "forbidden" with
     * ResponseStatusException; keep the status they chose instead of
     * collapsing everything into a 500.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView handleResponseStatus(ResponseStatusException ex) {
        HttpStatusCode status = ex.getStatusCode();
        String view;
        if (status.equals(HttpStatus.NOT_FOUND)) {
            view = "error/404";
        } else if (status.equals(HttpStatus.FORBIDDEN)) {
            view = "error/403";
        } else {
            view = "error/500";
        }
        ModelAndView mav = new ModelAndView(view);
        mav.setStatus(status);
        return mav;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleUnexpected(Exception ex, Model model) {
        log.error("Unhandled exception", ex);
        model.addAttribute("message", "Something went wrong. Please try again later.");
        return "error/500";
    }
}
