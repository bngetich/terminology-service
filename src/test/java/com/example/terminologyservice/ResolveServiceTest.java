package com.example.terminologyservice;

import com.example.terminologyservice.dto.ResolveResponse;
import com.example.terminologyservice.service.ResolveService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ResolveServiceTest {

    @Autowired
    ResolveService resolveService;

    @Test
    void matchesExactTermNorm() throws Exception {
        ResolveResponse res =
                resolveService.resolve("type 2 diabetes mellitus", "http://snomed.info/sct");

        assertEquals("matched", res.getStatus());
    }
}

