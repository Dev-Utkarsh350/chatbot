package com.example.pdfchat;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;

import java.util.List;
import java.util.stream.Collectors;

public class PdfChatEngine {

    private final OllamaChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final VectorStore store;

    public PdfChatEngine(String baseUrl, VectorStore store) {

        this.chatModel = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName("llama3.2")     // or mistral
                .build();

        this.embeddingModel = OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName("nomic-embed-text")  // embedding model
                .build();

        this.store = store;
    }

    public String ask(String question) {

        float[] qv = embeddingModel.embed(question).content().vector();
        List<VectorStore.ScoredChunk> matches = store.topK(qv, 5);

        String context = matches.stream()
                .map(c -> "[" + c.source() + "] " + c.text())
                .collect(Collectors.joining("\n---\n"));

        String prompt =
                "Use only this context to answer. If not present, reply 'I don't know'.\n" +
                        "Context:\n" + context + "\n\n" +
                        "Question: " + question + "\n" +
                        "Answer:";

        return chatModel.chat(prompt);
    }
}
