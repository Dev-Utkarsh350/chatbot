Summary

Local desktop chatbot that answers questions from a PDF.
Runs fully offline using Ollama. No API keys or cloud tokens.

What it does

User uploads a PDF.

Text is extracted (PDFBox).

Text is chunked (1000 chars, overlap 200).

Each chunk is embedded via Ollama (nomic-embed-text).

Chunks + vectors stored in SQLite.

On a question, query text is embedded, top similar chunks are retrieved using cosine similarity.

Ollama chat model (llama3.2) answers strictly from retrieved context.

UI shows chat in Swing.

Key components
File	Role
AppMain.java	Swing UI
PdfReader.java	Extracts PDF text
PdfIndexer.java	Chunks PDF and stores vectors
VectorStore.java / SqliteVectorStore.java	Chunk + embedding storage
PdfChatEngine.java	Retrieval + prompt → Ollama
Dependencies (pom)

pdfbox

langchain4j-core

langchain4j

langchain4j-ollama

sqlite-jdbc

swingx-all

Run
ollama serve
ollama pull llama3.2
ollama pull nomic-embed-text
mvn clean install


Run AppMain from IntelliJ.
