package com.desafio.obrigacoes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "documento")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrega_id", nullable = false)
    private Entrega entrega;

    @Column(name = "nome_arquivo", nullable = false)
    private String nomeArquivo;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "tamanho_bytes", nullable = false)
    private Long tamanhoBytes;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] conteudo;

    @Column(name = "enviado_em", nullable = false)
    private LocalDateTime enviadoEm;

    protected Documento() {
    }

    public Documento(Entrega entrega, String nomeArquivo, String contentType,
                     Long tamanhoBytes, byte[] conteudo, LocalDateTime enviadoEm) {
        this.entrega = entrega;
        this.nomeArquivo = nomeArquivo;
        this.contentType = contentType;
        this.tamanhoBytes = tamanhoBytes;
        this.conteudo = conteudo;
        this.enviadoEm = enviadoEm;
    }

    public Long getId() {
        return id;
    }

    public Entrega getEntrega() {
        return entrega;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getTamanhoBytes() {
        return tamanhoBytes;
    }

    public byte[] getConteudo() {
        return conteudo;
    }

    public LocalDateTime getEnviadoEm() {
        return enviadoEm;
    }
}
