package com.zbkj.common.exception;

import com.zbkj.common.response.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;


/**
 * @author Chennong
 * @date 2023-06-08
 * @description 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 拦截表单参数校验
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler({BindException.class})
    public CommonResult bindException(BindException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',参数校验异常.", requestURI, e);
        BindingResult bindingResult = e.getBindingResult();
        return CommonResult.failed(ExceptionCodeEnum.VALIDATE_FAILED, Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage());
    }

    /**
     * 拦截JSON参数校验
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResult bindException(MethodArgumentNotValidException e, HttpServletRequest request) {
        BindingResult bindingResult = e.getBindingResult();
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',JSON参数校验异常.", requestURI, e);
        return CommonResult.failed(ExceptionCodeEnum.VALIDATE_FAILED, Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage());
    }

    /**
     * 拦截参数类型不正确
     *
     * @param e
     * @return
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public CommonResult bindException(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',参数类型不正确.", requestURI, e);
        return CommonResult.failed(ExceptionCodeEnum.PRAM_NOT_MATCH, Objects.requireNonNull(e.getMessage()));
    }


    //声明要捕获的异常
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public <T> CommonResult<?> defaultExceptionHandler(Exception e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',系统异常.", requestURI, e);
        if (e instanceof CrmebException) {
            return CommonResult.failed(ExceptionCodeEnum.FAILED, Objects.requireNonNull(e.getMessage()));
        }
        if (e instanceof MissingServletRequestParameterException) {
            return CommonResult.failed(ExceptionCodeEnum.PRAM_NOT_MATCH, Objects.requireNonNull(e.getMessage()));
        }
        //未知错误
        return CommonResult.failed(ExceptionCodeEnum.ERROR, e.getMessage());
    }
}
