package com.desafio.obrigacoes.repository;

import com.desafio.obrigacoes.model.Entrega;
import com.desafio.obrigacoes.model.SituacaoEntrega;
import com.desafio.obrigacoes.model.TipoObrigacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO entrega
                (empresa_id, tipo, competencia, data_vencimento, situacao, versao, criado_em)
            VALUES
                (:empresaId, :tipo, :competencia, :dataVencimento, 'ABERTA', 0, :criadoEm)
            """, nativeQuery = true)
    int inserirSeNaoExistir(@Param("empresaId") Long empresaId,
                            @Param("tipo") String tipo,
                            @Param("competencia") String competencia,
                            @Param("dataVencimento") LocalDate dataVencimento,
                            @Param("criadoEm") LocalDateTime criadoEm);

    @Query("""
            select e from Entrega e join fetch e.empresa
            where (:empresaId is null or e.empresa.id = :empresaId)
              and (:tipo is null or e.tipo = :tipo)
              and (:competencia is null or e.competencia = :competencia)
              and (:situacao is null or e.situacao = :situacao)
            order by e.dataVencimento, e.id
            """)
    List<Entrega> buscar(@Param("empresaId") Long empresaId,
                         @Param("tipo") TipoObrigacao tipo,
                         @Param("competencia") String competencia,
                         @Param("situacao") SituacaoEntrega situacao);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Entrega e where e.id = :id")
    Optional<Entrega> buscarParaAlteracao(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = "empresa")
    Optional<Entrega> findById(Long id);
}
