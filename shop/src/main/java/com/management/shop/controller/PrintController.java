package com.management.shop.controller;

import com.management.shop.service.ThermalPrintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/print")
public class PrintController {

    @Autowired
    private ThermalPrintService printService;

    @PostMapping("/{orderId}")
    public String triggerPrint(@PathVariable Long orderId) {
        return printService.printOrder(orderId);
    }
}