package com.desafio.obrigacoes.repository;

import com.desafio.obrigacoes.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    boolean existsByCnpj(String cnpj);

    boolean existsByCnpjAndIdNot(String cnpj, Long id);

    java.util.List<Empresa> findByAtivaTrueOrderByIdAsc();

    java.util.List<Empresa> findAllByOrderByIdAsc();
}
