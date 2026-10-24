package com.desafio.obrigacoes.service;

import com.desafio.obrigacoes.exception.ConflitoException;
import com.desafio.obrigacoes.exception.DadosInvalidosException;
import com.desafio.obrigacoes.exception.RecursoNaoEncontradoException;
import com.desafio.obrigacoes.exception.RegraNegocioException;
import com.desafio.obrigacoes.model.Empresa;
import com.desafio.obrigacoes.model.EmpresaObrigacao;
import com.desafio.obrigacoes.model.TipoObrigacao;
import com.desafio.obrigacoes.repository.EmpresaObrigacaoRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmpresaObrigacaoService {

    private final EmpresaService empresaService;
    private final EmpresaObrigacaoRepository empresaObrigacaoRepository;
    private final Clock clock;

    public EmpresaObrigacaoService(
            EmpresaService empresaService,
            EmpresaObrigacaoRepository empresaObrigacaoRepository,
            Clock clock) {

        this.empresaService = empresaService;
        this.empresaObrigacaoRepository = empresaObrigacaoRepository;
        this.clock = clock;
    }

    public EmpresaObrigacao anexar(Long empresaId, TipoObrigacao tipo) {
        Empresa empresa = empresaService.buscarPorId(empresaId);

        if (tipo == null) {
            throw new DadosInvalidosException("Tipo de obrigação é obrigatório");
        }
        if (!tipo.aceita(empresa.getRegimeTributario())) {
            throw new RegraNegocioException(
                    "A obrigação " + tipo + " não se aplica ao regime " + empresa.getRegimeTributario());
        }
        if (empresaObrigacaoRepository.existsByEmpresaIdAndTipo(empresaId, tipo)) {
            throw new ConflitoException("A obrigação " + tipo + " já está anexada a esta empresa");
        }

        EmpresaObrigacao vinculo = new EmpresaObrigacao();
        vinculo.setEmpresa(empresa);
        vinculo.setTipo(tipo);
        vinculo.setCriadoEm(LocalDateTime.now(clock));
        return empresaObrigacaoRepository.save(vinculo);
    }

    public List<EmpresaObrigacao> listar(Long empresaId) {
        empresaService.buscarPorId(empresaId);
        return empresaObrigacaoRepository.findByEmpresaIdOrderByTipo(empresaId);
    }

    public void remover(Long empresaId, TipoObrigacao tipo) {
        empresaService.buscarPorId(empresaId);
        EmpresaObrigacao vinculo = empresaObrigacaoRepository.findByEmpresaIdAndTipo(empresaId, tipo)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "A obrigação " + tipo + " não está anexada a esta empresa"));
        empresaObrigacaoRepository.delete(vinculo);
    }
}
