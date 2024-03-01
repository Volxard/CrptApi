package ru.volxard;

import com.google.gson.Gson;
import lombok.*;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private static final String BASE_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client;
    private final Gson gson;
    private final Semaphore rateLimiter;

    private CrptApi(int requestLimit, long timeWindow, TimeUnit timeUnit) {
        this.client = new OkHttpClient();
        this.gson = new Gson();
        this.rateLimiter = new Semaphore(requestLimit, true);

        new Thread(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(timeUnit.toMillis(timeWindow));
                for (int i = 0; i < requestLimit; i++) {
                    rateLimiter.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private static class CrptApiHolder {
        private static final CrptApi INSTANCE = new CrptApi(10, 1, TimeUnit.SECONDS);
    }

    public static CrptApi getInstance() {
        return CrptApiHolder.INSTANCE;
    }

    public void createDocument(Document document, String signature) {
        if (!rateLimiter.tryAcquire()) {
            System.out.println("Rate limit exceeded. Request blocked.");
            return;
        }

        String json = gson.toJson(document);
        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
                .url(BASE_URL)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.out.println("Unexpected code " + response);
                return;
            }

            System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        CrptApi api = CrptApi.getInstance();

        Document document = new Document();
        document.setDescription("Description of the document");
        document.setDocId("12345");
        document.setDocStatus("Active");
        document.setDocType("LP_INTRODUCE_GOODS");
        document.setImportRequest(true);
        document.setOwnerInn("1234567890");
        document.setParticipantInn("0987654321");
        document.setProducerInn("1357924680");
        document.setProductionDate("2022-01-01");
        document.setProductionType("Production Type");
        document.setRegDate("2022-01-01");
        document.setRegNumber("Registration Number");

        Document.Product product = new Document.Product();
        product.setCertificateDocument("Certificate Document");
        product.setCertificateDocumentDate("2022-01-01");
        product.setCertificateDocumentNumber("Certificate Document Number");
        product.setOwnerInn("1234567890");
        product.setProducerInn("0987654321");
        product.setProductionDate("2022-01-01");
        product.setTnvedCode("TNVED Code");
        product.setUitCode("UIT Code");
        product.setUituCode("UITU Code");

        document.setProducts(new Document.Product[] { product });

        api.createDocument(document, "signature");
    }

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Document {
        private String description;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private String productionDate;
        private String productionType;
        private Product[] products;
        private String regDate;
        private String regNumber;


        @Setter
        @Getter
        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        public static class Product {
            private String certificateDocument;
            private String certificateDocumentDate;
            private String certificateDocumentNumber;
            private String ownerInn;
            private String producerInn;
            private String productionDate;
            private String tnvedCode;
            private String uitCode;
            private String uituCode;

        }
    }
}