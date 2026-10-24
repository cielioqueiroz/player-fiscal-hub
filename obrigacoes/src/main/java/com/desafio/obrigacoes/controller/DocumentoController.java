package com.desafio.obrigacoes.controller;

import com.desafio.obrigacoes.dto.DocumentoResponse;
import com.desafio.obrigacoes.model.Documento;
import com.desafio.obrigacoes.service.DocumentoService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@Tag(name = "Documentos", description = "Envio, consulta, download e exclusão de documentos")
public class DocumentoController {

    private final DocumentoService documentoService;

    public DocumentoController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @PostMapping(path = "/entregas/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponse> anexar(@PathVariable Long id,
                                                    @RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentoService.anexar(id, arquivo));
    }

    @GetMapping("/entregas/{id}/documentos")
    public List<DocumentoResponse> listar(@PathVariable Long id) {
        return documentoService.listar(id);
    }

    @DeleteMapping("/documentos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        documentoService.excluir(id);
    }

    @GetMapping("/documentos/{id}/download")
    public ResponseEntity<byte[]> baixar(@PathVariable Long id) {
        Documento d = documentoService.buscar(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(d.getNomeArquivo(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(d.getConteudo());
    }
}
