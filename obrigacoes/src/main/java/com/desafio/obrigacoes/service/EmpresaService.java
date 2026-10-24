package com.desafio.obrigacoes.service;

import com.desafio.obrigacoes.dto.EmpresaRequest;
import com.desafio.obrigacoes.exception.ConflitoException;
import com.desafio.obrigacoes.exception.DadosInvalidosException;
import com.desafio.obrigacoes.exception.RecursoNaoEncontradoException;
import com.desafio.obrigacoes.exception.RegraNegocioException;
import com.desafio.obrigacoes.model.Empresa;
import com.desafio.obrigacoes.model.RegimeTributario;
import com.desafio.obrigacoes.repository.EmpresaRepository;
import com.desafio.obrigacoes.repository.EmpresaObrigacaoRepository;
import com.desafio.obrigacoes.validation.CnpjValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmpresaService {

    private static final int TAMANHO_MAXIMO_NOME = 255;

    private final EmpresaRepository empresaRepository;
    private final EmpresaObrigacaoRepository empresaObrigacaoRepository;

    public EmpresaService(EmpresaRepository empresaRepository, EmpresaObrigacaoRepository empresaObrigacaoRepository) {
        this.empresaRepository = empresaRepository;
        this.empresaObrigacaoRepository = empresaObrigacaoRepository;
    }

    public List<Empresa> listarTodas(boolean incluirInativas) {
        return incluirInativas
                ? empresaRepository.findAllByOrderByIdAsc()
                : empresaRepository.findByAtivaTrueOrderByIdAsc();
    }

    public Empresa cadastrar(EmpresaRequest request) {
        DadosEmpresa dados = validar(request);
        String cnpj = dados.cnpj();
        if (empresaRepository.existsByCnpj(cnpj)) {
            throw new ConflitoException("Já existe uma empresa com o CNPJ " + cnpj);
        }

        Empresa empresa = new Empresa();
        empresa.setNome(dados.nome());
        empresa.setCnpj(cnpj);
        empresa.setRegimeTributario(dados.regimeTributario());
        return empresaRepository.save(empresa);
    }

    @Transactional
    public Empresa editar(Long id, EmpresaRequest request) {
        Empresa empresa = buscarPorId(id);
        DadosEmpresa dados = validar(request);
        if (empresaRepository.existsByCnpjAndIdNot(dados.cnpj(), id)) {
            throw new ConflitoException("Já existe uma empresa com o CNPJ " + dados.cnpj());
        }
        RegimeTributario novoRegime = dados.regimeTributario();
        if (novoRegime != empresa.getRegimeTributario()) {
            List<String> incompativeis = empresaObrigacaoRepository.findByEmpresaIdOrderByTipo(id).stream()
                    .filter(vinculo -> !vinculo.getTipo().aceita(novoRegime))
                    .map(vinculo -> vinculo.getTipo().name())
                    .toList();
            if (!incompativeis.isEmpty()) {
                throw new RegraNegocioException("Remova as obrigações incompatíveis antes de alterar o regime: "
                        + String.join(", ", incompativeis));
            }
        }
        empresa.setNome(dados.nome());
        empresa.setCnpj(dados.cnpj());
        empresa.setRegimeTributario(novoRegime);
        return empresaRepository.save(empresa);
    }

    public Empresa alterarAtiva(Long id, Boolean ativa) {
        if (ativa == null) {
            throw new DadosInvalidosException("ativa é obrigatória");
        }
        Empresa empresa = buscarPorId(id);
        empresa.setAtiva(ativa);
        return empresaRepository.save(empresa);
    }

    private DadosEmpresa validar(EmpresaRequest request) {
        if (request == null || request.nome() == null || request.nome().isBlank()) {
            throw new DadosInvalidosException("Nome é obrigatório");
        }
        String nome = request.nome().trim();
        if (nome.length() > TAMANHO_MAXIMO_NOME) {
            throw new DadosInvalidosException("Nome deve ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres");
        }
        if (!CnpjValidator.isValido(request.cnpj())) {
            throw new DadosInvalidosException("CNPJ inválido");
        }
        if (request.regimeTributario() == null) {
            throw new DadosInvalidosException("Regime tributário é obrigatório");
        }
        return new DadosEmpresa(nome, CnpjValidator.normalizar(request.cnpj()), request.regimeTributario());
    }

    private record DadosEmpresa(String nome, String cnpj, RegimeTributario regimeTributario) {}

    public Empresa buscarPorId(Long id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa " + id + " não encontrada"));
    }
}
