package com.desafio.obrigacoes.repository;

import com.desafio.obrigacoes.model.Entrega;
import com.desafio.obrigacoes.model.SituacaoEntrega;
import com.desafio.obrigacoes.model.TipoObrigacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Entrega e set e.situacao = :atrasada where e.situacao = :aberta and e.dataVencimento < :hoje")
    int atualizarAbertasVencidas(@Param("hoje") LocalDate hoje,
                                 @Param("aberta") SituacaoEntrega aberta,
                                 @Param("atrasada") SituacaoEntrega atrasada);

    @Query("""
            select new com.desafio.obrigacoes.dto.ResumoEntregasResponse(
                count(e),
                count(case when e.situacao = :aberta then 1 else null end),
                count(case when e.situacao = :atrasada then 1 else null end),
                count(case when e.situacao = :concluida then 1 else null end),
                count(case when e.situacao = :cancelada then 1 else null end),
                count(case when e.situacao = :aberta and e.dataVencimento between :hoje and :limite then 1 else null end),
                count(case when e.situacao = :aberta and e.dataVencimento < :hoje then 1 else null end)
            )
            from Entrega e
            where (:empresaId is null or e.empresa.id = :empresaId)
            """)
    com.desafio.obrigacoes.dto.ResumoEntregasResponse resumir(
            @Param("empresaId") Long empresaId,
            @Param("hoje") LocalDate hoje,
            @Param("limite") LocalDate limite,
            @Param("aberta") SituacaoEntrega aberta,
            @Param("atrasada") SituacaoEntrega atrasada,
            @Param("concluida") SituacaoEntrega concluida,
            @Param("cancelada") SituacaoEntrega cancelada);

    @EntityGraph(attributePaths = "empresa")
    @Query("""
            select e from Entrega e
            where (:empresaId is null or e.empresa.id = :empresaId)
              and (:tipo is null or e.tipo = :tipo)
              and (:competencia is null or e.competencia = :competencia)
              and (:situacao is null or e.situacao = :situacao)
            """)
    Page<Entrega> buscar(@Param("empresaId") Long empresaId,
                         @Param("tipo") TipoObrigacao tipo,
                         @Param("competencia") String competencia,
                         @Param("situacao") SituacaoEntrega situacao,
                         Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Entrega e where e.id = :id")
    Optional<Entrega> buscarParaAlteracao(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = "empresa")
    Optional<Entrega> findById(Long id);
}
