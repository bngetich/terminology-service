package com.example.terminologyservice.importer;

import com.example.terminologyservice.normalize.TermNormalizer;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class SnomedImportService {

    private static final String SYSTEM = "http://snomed.info/sct";
    private static final String FSN = "900000000000003001";
    private static final String SYNONYM = "900000000000013009";

    private static final int BATCH_SIZE = 1000;

    private final RestClient client;
    private final String index;

    public SnomedImportService(RestClient client,
                               @Value("${elasticsearch.index:terminology}") String index) {
        this.client = client;
        this.index = index;
    }

    public void importSnomed(String conceptFile,
                             String descriptionFile,
                             String version) throws Exception {

        Map<String, Boolean> activeConcepts = loadActiveConcepts(conceptFile);

        StringBuilder bulk = new StringBuilder();
        int count = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(descriptionFile))) {
            br.readLine(); // header

            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = line.split("\t");

                String active = cols[2];
                String conceptId = cols[4];
                String typeId = cols[6];
                String term = cols[7];

                if (!"1".equals(active)) continue;
                if (!Boolean.TRUE.equals(activeConcepts.get(conceptId))) continue;
                if (!FSN.equals(typeId) && !SYNONYM.equals(typeId)) continue;

                int rank = FSN.equals(typeId) ? 0 : 10;

                String json = String.format(
                        "{\"system\":\"%s\",\"version\":\"%s\",\"code\":\"%s\","
                                + "\"display\":\"%s\",\"text\":\"%s\",\"term_norm\":\"%s\","
                                + "\"rank\":%d,\"active\":true}",
                        SYSTEM,
                        version,
                        conceptId,
                        term,
                        term,
                        TermNormalizer.normalize(term),
                        rank
                );

                String docId = SYSTEM + "|" + conceptId + "|" + term;

                bulk.append("{\"index\":{\"_index\":\"")
                        .append(index)
                        .append("\",\"_id\":\"")
                        .append(URLEncoder.encode(docId, StandardCharsets.UTF_8))
                        .append("\"}}\n");

                bulk.append(json).append("\n");

                count++;
                if (count % BATCH_SIZE == 0){
                    flushBulk(bulk);
                    System.out.println("Indexed " + count + " SNOMED rows so far...");
                }
            }
        }

        flushBulk(bulk);
        System.out.println("SNOMED import finished.");
    }

    private Map<String, Boolean> loadActiveConcepts(String file) throws Exception {
        Map<String, Boolean> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = line.split("\t");
                map.put(cols[0], "1".equals(cols[2]));
            }
        }
        return map;
    }

    private void flushBulk(StringBuilder bulk) throws Exception {
        if (bulk.length() == 0) return;

        Request req = new Request("POST", "/_bulk");
        req.addParameter("refresh", "false");

        req.setEntity(
                new org.apache.http.nio.entity.NStringEntity(
                        bulk.toString(),
                        org.apache.http.entity.ContentType.create(
                                "application/x-ndjson",
                                StandardCharsets.UTF_8
                        )
                )
        );

        client.performRequest(req);
        bulk.setLength(0);
    }
}
