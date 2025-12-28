package com.example.terminologyservice.dto;

import java.util.List;
import java.util.Map;

public class ResolveResponse {

    private String status;
    private Map<String, Object> codeableConcept;
    private List<Map<String, Object>> candidates;

    public static ResolveResponse matched(Map<String, Object> cc) {
        ResolveResponse r = new ResolveResponse();
        r.status = "matched";
        r.codeableConcept = cc;
        return r;
    }

    public static ResolveResponse ambiguous(List<Map<String, Object>> cands) {
        ResolveResponse r = new ResolveResponse();
        r.status = "ambiguous";
        r.candidates = cands;
        return r;
    }

    public static ResolveResponse noMatch() {
        ResolveResponse r = new ResolveResponse();
        r.status = "no_match";
        return r;
    }

    public String getStatus() {
        return status;
    }

    public Map<String, Object> getCodeableConcept() {
        return codeableConcept;
    }

    public List<Map<String, Object>> getCandidates() {
        return candidates;
    }
}
