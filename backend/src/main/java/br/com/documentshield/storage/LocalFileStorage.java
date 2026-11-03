package br.com.documentshield.storage;

import br.com.documentshield.detection.extractor.PdfDocumentExtractor;
import br.com.documentshield.exception.InvalidDocumentException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorage {
    private final Path root;
    private final long max;
    private final PdfDocumentExtractor extractor;

    public LocalFileStorage(
            @Value("${document-shield.storage-directory:./data/documents}") String root,
            @Value("${document-shield.max-file-size:20971520}") long max,
            PdfDocumentExtractor extractor) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.max = max;
        this.extractor = extractor;
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível criar o diretório de armazenamento", e);
        }
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentException("Arquivo PDF é obrigatório");
        }
        if (file.getSize() > max) {
            throw new InvalidDocumentException("Arquivo excede 20 MB");
        }
        String original = safeName(file.getOriginalFilename());
        String stored = UUID.randomUUID() + ".pdf";
        Path target = resolve(stored), temp = resolve(stored + ".part");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long total = 0;
            byte[] first = new byte[5];
            int firstCount = 0;
            try (InputStream in = file.getInputStream();
                    OutputStream out = Files.newOutputStream(temp, StandardOpenOption.CREATE_NEW)) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    total += n;
                    if (total > max) {
                        throw new InvalidDocumentException("Arquivo excede 20 MB");
                    }
                    if (firstCount < 5) {
                        int take = Math.min(n, 5 - firstCount);
                        System.arraycopy(buffer, 0, first, firstCount, take);
                        firstCount += take;
                    }
                    digest.update(buffer, 0, n);
                    out.write(buffer, 0, n);
                }
            }
            if (firstCount < 5
                    || first[0] != '%'
                    || first[1] != 'P'
                    || first[2] != 'D'
                    || first[3] != 'F'
                    || first[4] != '-') {
                throw new InvalidDocumentException("Assinatura PDF inválida");
            }
            int pages = extractor.pageCount(temp);
            if (pages < 1) {
                throw new InvalidDocumentException("PDF sem páginas");
            }
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target);
            }
            return new StoredFile(
                    original, stored, target, total, HexFormat.of().formatHex(digest.digest()), pages);
        } catch (InvalidDocumentException e) {
            deleteQuietly(temp);
            throw e;
        } catch (Exception e) {
            deleteQuietly(temp);
            throw new InvalidDocumentException("PDF inválido, corrompido ou protegido por senha", e);
        }
    }

    public Path resolveStored(String stored) {
        Path p = resolve(stored);
        if (!Files.isRegularFile(p)) {
            throw new br.com.documentshield.exception.ResourceNotFoundException("Arquivo físico não encontrado");
        }
        return p;
    }

    public void delete(Path p) {
        Path n = p.toAbsolutePath().normalize();
        if (!n.startsWith(root)) {
            throw new IllegalArgumentException("Caminho fora do armazenamento");
        }
        try {
            Files.deleteIfExists(n);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao excluir arquivo armazenado", e);
        }
    }

    private Path resolve(String name) {
        Path p = root.resolve(name).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("Nome de arquivo inseguro");
        }
        return p;
    }

    private String safeName(String s) {
        if (s == null || s.isBlank()) {
            return "documento.pdf";
        }
        String n = s.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\u0000]", "_");
        return n.substring(0, Math.min(255, n.length()));
    }

    private void deleteQuietly(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException ignored) {
        }
    }
}
