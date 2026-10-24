package com.desafio.obrigacoes.service;

import com.desafio.obrigacoes.dto.DocumentoResponse;
import com.desafio.obrigacoes.exception.DadosInvalidosException;
import com.desafio.obrigacoes.exception.RecursoNaoEncontradoException;
import com.desafio.obrigacoes.exception.RegraNegocioException;
import com.desafio.obrigacoes.model.Documento;
import com.desafio.obrigacoes.model.Entrega;
import com.desafio.obrigacoes.repository.DocumentoRepository;
import com.desafio.obrigacoes.repository.EntregaRepository;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
public class DocumentoService {

    private static final long TAMANHO_MAXIMO = 10 * 1024 * 1024;
    private static final int TAMANHO_MAXIMO_NOME = 255;
    private static final int TAMANHO_MAXIMO_EXTENSAO = 20;

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "application/pdf", "image/png", "image/jpeg", "application/xml", "text/xml", "text/plain");

    private final DocumentoRepository documentoRepository;
    private final EntregaRepository entregaRepository;
    private final Clock clock;

    public DocumentoService(DocumentoRepository documentoRepository, EntregaRepository entregaRepository, Clock clock) {
        this.documentoRepository = documentoRepository;
        this.entregaRepository = entregaRepository;
        this.clock = clock;
    }

    @Transactional
    public DocumentoResponse anexar(Long entregaId, MultipartFile arquivo) {
        Entrega entrega = entregaRepository.buscarParaAlteracao(entregaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entrega " + entregaId + " não encontrada"));

        if (entrega.getSituacao().isFinal()) {
            throw new RegraNegocioException(
                    "Não é possível anexar documentos a uma entrega " + entrega.getSituacao());
        }
        if (arquivo == null || arquivo.isEmpty()) {
            throw new DadosInvalidosException("Arquivo é obrigatório");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new DadosInvalidosException("O arquivo excede o limite de 10 MB");
        }
        String contentTypeRecebido = arquivo.getContentType();
        String contentType = normalizarTipo(contentTypeRecebido);
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType)) {
            throw new DadosInvalidosException("Tipo de arquivo não permitido: " + contentTypeRecebido);
        }

        Documento documento = new Documento(entrega, limparNome(arquivo.getOriginalFilename()),
                contentType, arquivo.getSize(), lerBytes(arquivo), agora());

        documentoRepository.save(documento);
        return DocumentoResponse.de(documento);
    }

    public List<DocumentoResponse> listar(Long entregaId) {
        if (!entregaRepository.existsById(entregaId)) {
            throw new RecursoNaoEncontradoException("Entrega " + entregaId + " não encontrada");
        }
        return documentoRepository.listarResumo(entregaId);
    }

    public Documento buscar(Long id) {
        return documentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento " + id + " não encontrado"));
    }

    private static String normalizarTipo(String contentType) {
        if (contentType == null) {
            return null;
        }
        try {
            MediaType tipo = MediaType.parseMediaType(contentType);
            return tipo.getType() + "/" + tipo.getSubtype();
        } catch (InvalidMediaTypeException e) {
            return null;
        }
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    private static byte[] lerBytes(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o arquivo enviado", e);
        }
    }

    static String limparNome(String original) {
        if (original == null || original.isBlank()) {
            return "documento";
        }
        String semCaminho = original.replace('\\', '/');
        semCaminho = semCaminho.substring(semCaminho.lastIndexOf('/') + 1);
        String limpo = semCaminho.replaceAll("[\\p{Cc}\\p{Cf}]", "").trim();
        if (limpo.isEmpty()) {
            return "documento";
        }
        return cortar(limpo);
    }

    private static String cortar(String nome) {
        if (nome.codePointCount(0, nome.length()) <= TAMANHO_MAXIMO_NOME) {
            return nome;
        }
        int ponto = nome.lastIndexOf('.');
        String extensao = ponto > 0 ? nome.substring(ponto) : "";
        if (extensao.codePointCount(0, extensao.length()) > TAMANHO_MAXIMO_EXTENSAO) {
            extensao = "";
        }
        int espaco = TAMANHO_MAXIMO_NOME - extensao.codePointCount(0, extensao.length());
        return nome.substring(0, nome.offsetByCodePoints(0, espaco)) + extensao;
    }
}
