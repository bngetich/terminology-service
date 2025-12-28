package com.example.terminologyservice.importer;

import com.example.terminologyservice.normalize.TermNormalizer;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class RxNormImportService {

    private static final String SYSTEM = "http://www.nlm.nih.gov/research/umls/rxnorm";
    private static final int BATCH_SIZE = 1000;

    private final RestClient client;
    private final String index;

    public RxNormImportService(RestClient client,
                               @Value("${elasticsearch.index:terminology}") String index) {
        this.client = client;
        this.index = index;
    }

    public void importRxNorm(String rxnconsoFile, String version) throws Exception {

        StringBuilder bulk = new StringBuilder();
        int count = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(rxnconsoFile))) {

            String line;
            while ((line = br.readLine()) != null) {

                // RxNorm rows are pipe-delimited (keep blank values)
                String[] cols = line.split("\\|", -1);
                if (cols.length <= 16) continue;

                String rxCui = cols[0];
                String term = cols[14];
                String suppress = cols[16];

                // Skip suppressed rows
                if (!"N".equals(suppress)) continue;

                String termNorm = TermNormalizer.normalize(term);

                // One-line JSON (bulk API requires each doc on a single line)
                String json = String.format(
                        "{\"system\":\"%s\","
                                + "\"version\":\"%s\","
                                + "\"code\":\"%s\","
                                + "\"display\":\"%s\","
                                + "\"text\":\"%s\","
                                + "\"term_norm\":\"%s\","
                                + "\"rank\":0,"
                                + "\"active\":true}",
                        escapeJson(SYSTEM),
                        escapeJson(version),
                        escapeJson(rxCui),
                        escapeJson(term),
                        escapeJson(term),
                        escapeJson(termNorm)
                );

                // Stable hashed ID
                String rawId = SYSTEM + "|" + rxCui + "|" + version;
                String docId = sha256(rawId);

                // Bulk action line
                bulk.append("{\"index\":{\"_index\":\"")
                        .append(index)
                        .append("\",\"_id\":\"")
                        .append(docId)
                        .append("\"}}\n");

                // Bulk document line
                bulk.append(json).append("\n");

                count++;

                if (count % BATCH_SIZE == 0) {
                    flushBulk(bulk);
                    System.out.println("Indexed " + count + " RxNorm rows so far...");
                }
            }
        }

        flushBulk(bulk);
        System.out.println("RxNorm import complete. Total indexed: " + count);
    }

    private void flushBulk(StringBuilder bulk) throws Exception {
        if (bulk.length() == 0) return;

        Request req = new Request("POST", "/_bulk");
        req.addParameter("refresh", "false");

        req.setEntity(new org.apache.http.nio.entity.NStringEntity(
                bulk.toString(),
                org.apache.http.entity.ContentType.APPLICATION_JSON
        ));

        Response res = client.performRequest(req);

        if (res.getStatusLine().getStatusCode() >= 300) {
            throw new RuntimeException("Bulk indexing failed: " + res.getStatusLine());
        }

        bulk.setLength(0);
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash ID", e);
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}
