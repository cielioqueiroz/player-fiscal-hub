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
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
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

        byte[] conteudo = lerBytes(arquivo);
        validarConteudo(contentType, conteudo);

        Documento documento = new Documento(entrega, limparNome(arquivo.getOriginalFilename()),
                contentType, arquivo.getSize(), conteudo, agora());

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

    @Transactional
    public void excluir(Long id) {
        Documento documento = buscar(id);
        Long entregaId = documento.getEntrega().getId();
        Entrega entrega = entregaRepository.buscarParaAlteracao(entregaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entrega " + entregaId + " não encontrada"));
        if (entrega.getSituacao().isFinal()) {
            throw new RegraNegocioException("Não é possível excluir documentos de uma entrega " + entrega.getSituacao());
        }
        documentoRepository.delete(documento);
    }

    static void validarConteudo(String contentType, byte[] conteudo) {
        boolean valido = switch (contentType) {
            case "application/pdf" -> comecaCom(conteudo, new byte[] {'%', 'P', 'D', 'F'});
            case "image/png" -> comecaCom(conteudo,
                    new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
            case "image/jpeg" -> comecaCom(conteudo, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "application/xml", "text/xml" -> xmlValido(conteudo);
            case "text/plain" -> textoUtf8Valido(conteudo);
            default -> false;
        };
        if (!valido) {
            throw new DadosInvalidosException("O conteúdo do arquivo não corresponde ao tipo declarado");
        }
    }

    private static boolean xmlValido(byte[] conteudo) {
        String texto = decodificarUtf8(conteudo);
        if (texto == null) return false;
        String inicio = texto.stripLeading();
        return inicio.startsWith("<?xml") || inicio.startsWith("<");
    }

    private static boolean textoUtf8Valido(byte[] conteudo) {
        for (byte b : conteudo) {
            if (b == 0) return false;
        }
        return decodificarUtf8(conteudo) != null;
    }

    private static String decodificarUtf8(byte[] conteudo) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(conteudo)).toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private static boolean comecaCom(byte[] conteudo, byte[] assinatura) {
        if (conteudo.length < assinatura.length) return false;
        for (int i = 0; i < assinatura.length; i++) {
            if (conteudo[i] != assinatura[i]) return false;
        }
        return true;
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
