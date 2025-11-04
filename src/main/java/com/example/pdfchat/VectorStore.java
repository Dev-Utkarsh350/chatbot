package com.example.pdfchat;

import java.util.List;

public interface VectorStore {
    void initialize();
    long add(String chunkId, String text, float[] vector, String source);
    List<ScoredChunk> topK(float[] queryVector, int k);

    record ScoredChunk(String chunkId, String text, String source, double score) {}
}
