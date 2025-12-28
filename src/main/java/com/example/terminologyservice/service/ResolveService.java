package com.example.terminologyservice.service;

import com.example.terminologyservice.dto.ResolveResponse;
import com.example.terminologyservice.normalize.TermNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ResolveService {

    private final RestClient client;
    private final String index;
    private final ObjectMapper mapper = new ObjectMapper();

    public ResolveService(RestClient client,
                          @Value("${elasticsearch.index:terminology}") String index) {
        this.client = client;
        this.index = index;
    }

    public ResolveResponse resolve(String text, String system) throws Exception {

        String norm = TermNormalizer.normalize(text);

        String query = """
        {
          "query": {
            "bool": {
              "must": [
                { "term": { "term_norm": "%s" } },
                { "term": { "system": "%s" } },
                { "term": { "active": true } }
              ]
            }
          },
          "sort": [{ "rank": "asc" }],
          "size": 10
        }
        """.formatted(norm, system);

        Request req = new Request("POST", "/" + index + "/_search");
        req.setJsonEntity(query);

        Response res = client.performRequest(req);
        InputStream is = res.getEntity().getContent();
        String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        Map<?, ?> parsed = mapper.readValue(json, Map.class);
        List<Map<String, Object>> hits =
                (List<Map<String, Object>>) ((Map<?, ?>) parsed.get("hits")).get("hits");

        if (hits.isEmpty()) return ResolveResponse.noMatch();

        if (hits.size() == 1) {
            Map<String, Object> src = (Map<String, Object>) hits.get(0).get("_source");
            return ResolveResponse.matched(toCodeableConcept(src, text));
        }

        List<Map<String, Object>> candidates = new ArrayList<>();
        for (Map<String, Object> hit : hits) {
            Map<String, Object> src = (Map<String, Object>) hit.get("_source");
            candidates.add(toCodeableConcept(src, text));
        }

        return ResolveResponse.ambiguous(candidates);
    }

    private Map<String, Object> toCodeableConcept(Map<String, Object> src, String originalText) {
        return Map.of(
                "coding", List.of(
                        Map.of(
                                "system", src.get("system"),
                                "code", src.get("code"),
                                "display", src.get("display")
                        )
                ),
                "text", originalText
        );
    }
}
