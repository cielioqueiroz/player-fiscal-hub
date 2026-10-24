package com.desafio.obrigacoes.repository;

import com.desafio.obrigacoes.dto.DocumentoResponse;
import com.desafio.obrigacoes.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    @Query("""
            select new com.desafio.obrigacoes.dto.DocumentoResponse(
                d.id, d.nomeArquivo, d.contentType, d.tamanhoBytes, d.enviadoEm)
            from Documento d
            where d.entrega.id = :entregaId
            order by d.enviadoEm, d.id
            """)
    List<DocumentoResponse> listarResumo(@Param("entregaId") Long entregaId);
}
