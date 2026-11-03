package br.com.documentshield.service;

import br.com.documentshield.converter.ApiConverter;
import br.com.documentshield.detection.engine.DetectionEngine;
import br.com.documentshield.domain.entity.AnaliseDocumento;
import br.com.documentshield.domain.entity.Documento;
import br.com.documentshield.domain.entity.Ocorrencia;
import br.com.documentshield.domain.enums.StatusAnalise;
import br.com.documentshield.domain.enums.StatusDocumento;
import br.com.documentshield.dto.response.DocumentoResponse;
import br.com.documentshield.dto.response.UploadResponse;
import br.com.documentshield.exception.ProcessingCapacityException;
import br.com.documentshield.exception.ResourceNotFoundException;
import br.com.documentshield.repository.AnaliseDocumentoRepository;
import br.com.documentshield.repository.DocumentoRepository;
import br.com.documentshield.repository.EvidenciaRepository;
import br.com.documentshield.repository.ExecucaoRegraRepository;
import br.com.documentshield.repository.OcorrenciaRepository;
import br.com.documentshield.storage.LocalFileStorage;
import br.com.documentshield.storage.StoredFile;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {
    private final DocumentoRepository documents;
    private final AnaliseDocumentoRepository analyses;
    private final OcorrenciaRepository findings;
    private final EvidenciaRepository evidence;
    private final ExecucaoRegraRepository executions;
    private final LocalFileStorage storage;
    private final AnalysisPersistenceService persistence;
    private final AnalysisProcessor processor;
    private final DetectionEngine engine;
    private final ApiConverter converter;

    public DocumentService(
            DocumentoRepository d,
            AnaliseDocumentoRepository a,
            OcorrenciaRepository f,
            EvidenciaRepository e,
            ExecucaoRegraRepository x,
            LocalFileStorage s,
            AnalysisPersistenceService p,
            AnalysisProcessor pr,
            DetectionEngine en,
            ApiConverter c) {
        documents = d;
        analyses = a;
        findings = f;
        evidence = e;
        executions = x;
        storage = s;
        persistence = p;
        processor = pr;
        engine = en;
        converter = c;
    }

    public UploadResponse upload(MultipartFile file) {
        StoredFile stored = storage.store(file);
        Documento d = null;
        try {
            d = documents.save(Documento.builder()
                    .nomeOriginal(stored.originalName())
                    .nomeArmazenado(stored.storedName())
                    .tipoMime("application/pdf")
                    .extensao("pdf")
                    .tamanhoBytes(stored.size())
                    .hashSha256(stored.sha256())
                    .caminhoArmazenamento(stored.path().toString())
                    .quantidadePaginas(stored.pages())
                    .status(StatusDocumento.RECEBIDO)
                    .build());
            d.setStatus(StatusDocumento.VALIDANDO);
            d = documents.save(d);
            AnaliseDocumento a = persistence.create(d.getId(), engine.version());
            submitOrFail(a.getId());
            return new UploadResponse(
                    d.getId(), a.getId(), d.getStatus(), a.getStatus(), "/api/v1/analises/" + a.getId());
        } catch (RuntimeException e) {
            if (d == null) {
                storage.delete(stored.path());
            } else {
                d.setStatus(StatusDocumento.ERRO);
                d.setMensagemErro("Falha ao criar ou enfileirar a análise.");
                documents.save(d);
            }
            throw e;
        }
    }

    public DocumentoResponse get(UUID id) {
        return converter.toResponse(entity(id));
    }

    public Page<Documento> list(StatusDocumento status, Pageable p) {
        return status == null ? documents.findAll(p) : documents.findByStatus(status, p);
    }

    public FileDownload download(UUID id) {
        Documento d = entity(id);
        Path p = storage.resolveStored(d.getNomeArmazenado());
        return new FileDownload(new FileSystemResource(p), d.getNomeOriginal(), d.getTamanhoBytes());
    }

    public UploadResponse reanalyze(UUID id) {
        Documento d = entity(id);
        if (analyses.existsByDocumentoIdAndStatusIn(id, List.of(StatusAnalise.PENDENTE, StatusAnalise.EXECUTANDO))) {
            throw new IllegalArgumentException("Já existe uma análise em andamento para o documento");
        }
        d.setStatus(StatusDocumento.VALIDANDO);
        d.setMensagemErro(null);
        documents.save(d);
        AnaliseDocumento a = persistence.create(id, engine.version());
        submitOrFail(a.getId());
        return new UploadResponse(id, a.getId(), d.getStatus(), a.getStatus(), "/api/v1/analises/" + a.getId());
    }

    @Transactional
    public void delete(UUID id) {
        Documento d = entity(id);
        if (analyses.existsByDocumentoIdAndStatusIn(id, List.of(StatusAnalise.PENDENTE, StatusAnalise.EXECUTANDO))) {
            throw new IllegalArgumentException("Não é possível excluir um documento com análise em andamento");
        }
        List<AnaliseDocumento> list =
                analyses.findByDocumentoId(id, Pageable.unpaged()).getContent();
        for (AnaliseDocumento a : list) {
            List<Ocorrencia> os = findings.findAllByAnaliseId(a.getId());
            evidence.deleteByOcorrenciaIdIn(os.stream().map(Ocorrencia::getId).toList());
            findings.deleteByAnaliseId(a.getId());
            executions.deleteByAnaliseId(a.getId());
            analyses.delete(a);
        }
        documents.delete(d);
        Path path = Path.of(d.getCaminhoArmazenamento());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            public void afterCommit() {
                storage.delete(path);
            }
        });
    }

    private void submitOrFail(UUID id) {
        try {
            processor.process(id);
        } catch (RejectedExecutionException e) {
            persistence.fail(id, "Fila de processamento cheia");
            throw new ProcessingCapacityException("A fila de análise está cheia; tente novamente mais tarde.");
        }
    }

    private Documento entity(UUID id) {
        return documents
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado: " + id));
    }

    public record FileDownload(Resource resource, String filename, long size) {}
}
