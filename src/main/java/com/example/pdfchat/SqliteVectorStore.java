package com.example.pdfchat;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqliteVectorStore implements VectorStore {
    private final String url;

    public SqliteVectorStore(String dbPath) {
        this.url = "jdbc:sqlite:" + dbPath;
    }

    @Override
    public void initialize() {
        try (Connection c = DriverManager.getConnection(url);
             Statement st = c.createStatement()) {
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS chunks (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      chunk_id TEXT UNIQUE,
                      text TEXT NOT NULL,
                      source TEXT
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS embeddings (
                      chunk_id TEXT PRIMARY KEY,
                      vector BLOB NOT NULL
                    )
                    """);
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_chunks_source ON chunks(source)");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long add(String chunkId, String text, float[] vector, String source) {
        try (Connection c = DriverManager.getConnection(url)) {
            c.setAutoCommit(false);
            try (PreparedStatement ps1 = c.prepareStatement(
                    "INSERT OR REPLACE INTO chunks(chunk_id,text,source) VALUES(?,?,?)");
                 PreparedStatement ps2 = c.prepareStatement(
                         "INSERT OR REPLACE INTO embeddings(chunk_id,vector) VALUES(?,?)")) {
                ps1.setString(1, chunkId);
                ps1.setString(2, text);
                ps1.setString(3, source);
                ps1.executeUpdate();

                ps2.setString(1, chunkId);
                ps2.setBytes(2, floatArrayToBytes(vector));
                ps2.executeUpdate();

                c.commit();
                return 1;
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ScoredChunk> topK(float[] queryVector, int k) {
        try (Connection c = DriverManager.getConnection(url);
             PreparedStatement ps = c.prepareStatement(
                     "SELECT chunks.chunk_id, chunks.text, chunks.source, embeddings.vector " +
                             "FROM chunks JOIN embeddings ON chunks.chunk_id = embeddings.chunk_id")) {
            ResultSet rs = ps.executeQuery();
            List<ScoredChunk> scored = new ArrayList<>();
            while (rs.next()) {
                String chunkId = rs.getString(1);
                String text = rs.getString(2);
                String source = rs.getString(3);
                byte[] blob = rs.getBytes(4);
                float[] v = bytesToFloatArray(blob);
                double score = cosine(queryVector, v);
                scored.add(new ScoredChunk(chunkId, text, source, score));
            }
            scored.sort((a, b) -> Double.compare(b.score(), a.score()));
            return scored.subList(0, Math.min(k, scored.size()));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private static byte[] floatArrayToBytes(float[] arr) {
        byte[] out = new byte[arr.length * 4];
        for (int i = 0; i < arr.length; i++) {
            int bits = Float.floatToIntBits(arr[i]);
            int o = i * 4;
            out[o] = (byte) (bits >>> 24);
            out[o + 1] = (byte) (bits >>> 16);
            out[o + 2] = (byte) (bits >>> 8);
            out[o + 3] = (byte) (bits);
        }
        return out;
    }

    private static float[] bytesToFloatArray(byte[] bytes) {
        int n = bytes.length / 4;
        float[] arr = new float[n];
        for (int i = 0; i < n; i++) {
            int o = i * 4;
            int bits = ((bytes[o] & 0xFF) << 24)
                    | ((bytes[o + 1] & 0xFF) << 16)
                    | ((bytes[o + 2] & 0xFF) << 8)
                    | (bytes[o + 3] & 0xFF);
            arr[i] = Float.intBitsToFloat(bits);
        }
        return arr;
    }
}
