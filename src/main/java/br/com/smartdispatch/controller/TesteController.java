package br.com.smartdispatch.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TesteController {

    @GetMapping("/teste")
    public String testarAplicacao() {
        return "Smart Dispatch online";
    }
}