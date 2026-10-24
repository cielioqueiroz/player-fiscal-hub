package com.desafio.obrigacoes.service;

import com.desafio.obrigacoes.dto.EmpresaRequest;
import com.desafio.obrigacoes.exception.ConflitoException;
import com.desafio.obrigacoes.exception.DadosInvalidosException;
import com.desafio.obrigacoes.exception.RecursoNaoEncontradoException;
import com.desafio.obrigacoes.model.Empresa;
import com.desafio.obrigacoes.repository.EmpresaRepository;
import com.desafio.obrigacoes.validation.CnpjValidator;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmpresaService {

    private static final int TAMANHO_MAXIMO_NOME = 255;

    private final EmpresaRepository empresaRepository;

    public EmpresaService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public List<Empresa> listarTodas() {
        return empresaRepository.findAll();
    }

    public Empresa cadastrar(EmpresaRequest request) {
        if (request.nome() == null || request.nome().isBlank()) {
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

        String cnpj = CnpjValidator.normalizar(request.cnpj());
        if (empresaRepository.existsByCnpj(cnpj)) {
            throw new ConflitoException("Já existe uma empresa com o CNPJ " + cnpj);
        }

        Empresa empresa = new Empresa();
        empresa.setNome(nome);
        empresa.setCnpj(cnpj);
        empresa.setRegimeTributario(request.regimeTributario());
        return empresaRepository.save(empresa);
    }

    public Empresa buscarPorId(Long id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa " + id + " não encontrada"));
    }
}
