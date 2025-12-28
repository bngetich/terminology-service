package com.example.terminologyservice.controller;

import com.example.terminologyservice.dto.ResolveRequest;
import com.example.terminologyservice.dto.ResolveResponse;
import com.example.terminologyservice.service.ResolveService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/terminology")
public class ResolveController {

    private final ResolveService service;

    public ResolveController(ResolveService service){
        this.service = service;
    }

    @PostMapping("/resolve")
    public ResolveResponse resolve(@Valid @RequestBody ResolveRequest req)
            throws Exception {
        return service.resolve(req.getText(), req.getSystem());
    }
}
