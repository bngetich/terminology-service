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

@Service
public class LoincImportService {

    private static final String SYSTEM = "http://loinc.org";
    private static final int BATCH_SIZE = 1000;

    private final RestClient client;
    private final String index;

    public LoincImportService(
            RestClient client,
            @Value("${elasticsearch.index:terminology}") String index
    ) {
        this.client = client;
        this.index = index;
    }

    public void importLoinc(String loincFile, String version) throws Exception {

        StringBuilder bulk = new StringBuilder();
        int count = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(loincFile))) {

            br.readLine();   // skip header

            String line;
            while ((line = br.readLine()) != null) {

                // Split CSV safely for THIS dataset (15 cols)
                String[] cols = line.split("\",\"");

                // 0 = LOINC_NUM
                // 1 = COMPONENT
                // 9 = LONG_COMMON_NAME

                String code = cols[0].replace("\"", "");
                String component = cols[1];
                String longName = cols[9].replace("\"", "");

                String json = String.format(
                        "{\"system\":\"%s\",\"version\":\"%s\",\"code\":\"%s\","
                                + "\"display\":\"%s\",\"text\":\"%s\",\"term_norm\":\"%s\","
                                + "\"rank\":%d,\"active\":true}",
                        SYSTEM,
                        version,
                        code,
                        longName,
                        longName,
                        TermNormalizer.normalize(component),
                        0
                );

                String docId = SYSTEM + "|" + code + "|" + longName;

                bulk.append("{\"index\":{\"_index\":\"")
                        .append(index)
                        .append("\",\"_id\":\"")
                        .append(URLEncoder.encode(docId, StandardCharsets.UTF_8))
                        .append("\"}}\n");

                bulk.append(json).append("\n");

                count++;

                if (count % BATCH_SIZE == 0) {
                    flushBulk(bulk);
                    System.out.println("Indexed " + count + " LOINC rows so far...");
                }
            }
        }

        flushBulk(bulk);
        System.out.println("LOINC import complete. Total indexed: " + count);
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

        Response res = client.performRequest(req);

        bulk.setLength(0);
    }
}
