package com.k8s.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yilan.hu
 * @create 2020/10/25 0:37
 **/
@Slf4j
@RestController
public class TestController {

    @Value("${version}")
    private String version;

    @GetMapping("/demo")
    public String test(@RequestParam(required = false) String text) {
        log.info("Version: {}, text: {}", version, text);
        return "Version: " + version + "\n" + text;
    }
}
