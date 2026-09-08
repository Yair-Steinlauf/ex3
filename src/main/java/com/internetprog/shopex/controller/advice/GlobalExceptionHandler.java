package com.internetprog.shopex.controller.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

    /**
     * A path variable or query parameter that cannot be converted to the
     * declared type (e.g. /products/abc, ?page=abc) is a malformed request,
     * not a server fault.
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest() {
        return "error/400";
    }

    /**
     * A POST-only endpoint reached with GET (a saved URL, a crawler) is a client
     * mistake. Without this it fell through to the catch-all below, which
     * answered 500 and logged a full stack trace for what is routine traffic.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public String handleMethodNotAllowed() {
        return "error/405";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return "error/500";
    }
}
