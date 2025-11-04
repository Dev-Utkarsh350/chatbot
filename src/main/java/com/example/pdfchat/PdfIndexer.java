package com.example.pdfchat;

import dev.langchain4j.model.ollama.OllamaEmbeddingModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PdfIndexer {
    private final OllamaEmbeddingModel embeddingModel;
    private final VectorStore store;

    public PdfIndexer(VectorStore store) {
        this.embeddingModel = OllamaEmbeddingModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("nomic-embed-text")
                .build();
        this.store = store;
    }

    public void index(File pdf) throws Exception {
        String text = PdfReader.extractText(pdf);
        List<String> chunks = split(text, 1000, 200); // size=1000, overlap=200

        for (String chunk : chunks) {
            float[] vec = embeddingModel.embed(chunk).content().vector();
            store.add(UUID.randomUUID().toString(), chunk, vec, pdf.getName());
        }
    }

    // Simple character-based splitter with overlap
    private static List<String> split(String text, int chunkSize, int overlap) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        if (chunkSize <= 0) throw new IllegalArgumentException("chunkSize > 0");
        if (overlap < 0 || overlap >= chunkSize) throw new IllegalArgumentException("0 <= overlap < chunkSize");

        int n = text.length();
        int start = 0;
        while (start < n) {
            int end = Math.min(start + chunkSize, n);
            out.add(text.substring(start, end));
            if (end == n) break;
            start = end - overlap;
        }
        return out;
    }
}
