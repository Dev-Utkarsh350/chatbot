package com.example.pdfchat;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.concurrent.CompletableFuture;

public class App {
    private static PdfChatEngine engine;
    private static VectorStore store;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::createAndShowGui);
    }

    private static void createAndShowGui() {
        JFrame frame = new JFrame("PDF ChatBot (Local)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 650);

        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        JTextField inputField = new JTextField();
        JButton sendBtn = new JButton("Send");
        JButton uploadBtn = new JButton("Index PDF");

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(uploadBtn, BorderLayout.WEST);
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendBtn, BorderLayout.EAST);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottom, BorderLayout.SOUTH);

        // Initialize local store and engine
        store = new SqliteVectorStore("./pdfchat.db");
        store.initialize();
        engine = new PdfChatEngine("http://localhost:11434", store);

        uploadBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int res = chooser.showOpenDialog(frame);
            if (res == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                chatArea.append("Indexing: " + file.getName() + "\n");
                CompletableFuture.runAsync(() -> {
                    try {
                        PdfIndexer indexer = new PdfIndexer(store);
                        indexer.index(file);
                        SwingUtilities.invokeLater(() -> chatArea.append("Indexed. You can ask questions now.\n"));
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> chatArea.append("Index failed: " + ex.getMessage() + "\n"));
                    }
                });
            }
        });

        sendBtn.addActionListener(e -> sendQuestion(inputField, chatArea));
        inputField.addActionListener(e -> sendQuestion(inputField, chatArea));

        frame.setVisible(true);
    }

    private static void sendQuestion(JTextField inputField, JTextArea chatArea) {
        String q = inputField.getText().trim();
        if (q.isEmpty()) return;
        chatArea.append("You: " + q + "\n");
        inputField.setText("");

        CompletableFuture.supplyAsync(() -> {
            try {
                return engine.ask(q);
            } catch (Exception ex) {
                return "ERROR: " + ex.getMessage();
            }
        }).thenAccept(answer ->
                SwingUtilities.invokeLater(() -> chatArea.append("Bot: " + answer + "\n\n"))
        );
    }
}
