package br.com.documentshield.storage;

import java.nio.file.Path;

public record StoredFile(String originalName, String storedName, Path path, long size, String sha256, int pages) {
}
