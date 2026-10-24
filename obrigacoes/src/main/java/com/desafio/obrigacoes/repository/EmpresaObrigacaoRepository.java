package com.desafio.obrigacoes.repository;

import com.desafio.obrigacoes.model.EmpresaObrigacao;
import com.desafio.obrigacoes.model.TipoObrigacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmpresaObrigacaoRepository extends JpaRepository<EmpresaObrigacao, Long> {

    boolean existsByEmpresaIdAndTipo(Long empresaId, TipoObrigacao tipo);

    List<EmpresaObrigacao> findByEmpresaIdOrderByTipo(Long empresaId);

    Optional<EmpresaObrigacao> findByEmpresaIdAndTipo(Long empresaId, TipoObrigacao tipo);

    List<EmpresaObrigacao> findAllByOrderByEmpresaIdAscTipoAsc();

    List<EmpresaObrigacao> findAllByEmpresaAtivaTrueOrderByEmpresaIdAscTipoAsc();
}
