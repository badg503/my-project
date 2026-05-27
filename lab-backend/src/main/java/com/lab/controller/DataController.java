package com.lab.controller;

import com.lab.common.Result;
import com.lab.util.DataGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/data")
public class DataController {

    @Autowired
    private DataGenerator dataGenerator;

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @PostMapping("/generate")
    public Result<Void> generateRandomData() {
        dataGenerator.generateRandomData();
        return Result.ok();
    }
}
