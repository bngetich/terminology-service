package com.example.terminologyservice.controller;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final RestClient client;

    public HealthController(RestClient client) {
        this.client = client;
    }

    @GetMapping("/health/es")
    public String checkEs() throws Exception {
        Request req = new Request("GET", "/");
        Response res = client.performRequest(req);
        return "Elasticsearch OK";
    }
}
