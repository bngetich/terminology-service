package com.example.terminologyservice.service;

import com.example.terminologyservice.dto.ResolveResponse;
import com.example.terminologyservice.normalize.TermNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        // Layer 1: exact term_norm
        String termNormQuery = """
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

        List<Map<String, Object>> hits = runQuery(termNormQuery);


        if (!hits.isEmpty()) {
            System.out.println("[resolve] hit layer=term_norm count=" + hits.size());
            return buildResponse(hits, text);
        }

        // Layer 2: exact text (keyword)
        String exactTextQuery = """
                {
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "text.keyword": "%s" } },
                        { "term": { "system": "%s" } },
                        { "term": { "active": true } }
                      ]
                    }
                  },
                  "sort": [{ "rank": "asc" }],
                  "size": 10
                }
                """.formatted(text, system);

        hits = runQuery(exactTextQuery);

        if (!hits.isEmpty()) {
            System.out.println("[resolve] hit layer=text.keyword count=" + hits.size());
            return buildResponse(hits, text);
        }


        // Layer 3: broader match on display (lexical, still deterministic)
        String matchDisplayQuery = """
                {
                  "query": {
                    "bool": {
                      "must": [
                        { "match": { "display": { "query": "%s", "operator": "and" } } },
                        { "term": { "system": "%s" } },
                        { "term": { "active": true } }
                      ]
                    }
                  },
                  "sort": [{ "rank": "asc" }],
                  "size": 10
                }
                """.formatted(text, system);

        hits = runQuery(matchDisplayQuery);

        if (!hits.isEmpty()) {
            System.out.println("[resolve] hit layer=display.match count=" + hits.size());
            return buildResponse(hits, text);
        }

        // Layer 4: controlled fuzzy search
        String fuzzyQuery = """
                {
                  "query": {
                    "bool": {
                      "must": [
                        {
                          "match": {
                            "display": {
                              "query": "%s",
                              "fuzziness": "AUTO",
                              "operator": "and"
                            }
                          }
                        },
                        { "term": { "system": "%s" } },
                        { "term": { "active": true } }
                      ]
                    }
                  },
                  "size": 5
                }
                """.formatted(text, system);

        hits = runQuery(fuzzyQuery);

        if (!hits.isEmpty()) {
            System.out.println("[resolve] hit layer=fuzzy count=" + hits.size());
            // Fuzzy is ALWAYS ambiguous — never auto-resolve
            return ResolveResponse.ambiguous(buildCandidates(hits, text));
        }


        // Nothing
        System.out.println("[resolve] no_match for text=\"" + text + "\" system=" + system);
        return ResolveResponse.noMatch();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildCandidates(List<Map<String, Object>> hits, String originalText) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> hit : hits) {
            Map<String, Object> src = (Map<String, Object>) hit.get("_source");
            out.add(toCodeableConcept(src, originalText));
        }
        return out;
    }


    @SuppressWarnings("unchecked")
    private ResolveResponse buildResponse(List<Map<String, Object>> hits, String originalText) {

        if (hits.size() == 1) {
            Map<String, Object> src =
                    (Map<String, Object>) hits.get(0).get("_source");
            return ResolveResponse.matched(toCodeableConcept(src, originalText));
        }

        List<Map<String, Object>> candidates = new ArrayList<>();
        for (Map<String, Object> hit : hits) {
            Map<String, Object> src =
                    (Map<String, Object>) hit.get("_source");
            candidates.add(toCodeableConcept(src, originalText));
        }
        return ResolveResponse.ambiguous(candidates);
    }


    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> runQuery(String jsonQuery) throws Exception {
        Request req = new Request("POST", "/" + index + "/_search");
        req.setJsonEntity(jsonQuery);

        Response res = client.performRequest(req);
        String body = new String(
                res.getEntity().getContent().readAllBytes(),
                StandardCharsets.UTF_8
        );

        Map<String, Object> parsed = mapper.readValue(body, Map.class);
        Map<String, Object> hitsObj = (Map<String, Object>) parsed.get("hits");

        return (List<Map<String, Object>>) hitsObj.get("hits");
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
