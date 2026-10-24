package com.desafio.obrigacoes.config;

import com.desafio.obrigacoes.model.Empresa;
import com.desafio.obrigacoes.model.EmpresaObrigacao;
import com.desafio.obrigacoes.model.Entrega;
import com.desafio.obrigacoes.model.RegimeTributario;
import com.desafio.obrigacoes.model.TipoObrigacao;
import com.desafio.obrigacoes.repository.EmpresaObrigacaoRepository;
import com.desafio.obrigacoes.repository.EmpresaRepository;
import com.desafio.obrigacoes.repository.EntregaRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;

@Configuration
@Profile("demo")
public class DemoDataSeeder {

    @Bean
    ApplicationRunner popularBancoVazio(EmpresaRepository empresaRepository,
                                        EmpresaObrigacaoRepository empresaObrigacaoRepository,
                                        EntregaRepository entregaRepository,
                                        Clock clock) {
        return new DemoDataRunner(empresaRepository, empresaObrigacaoRepository, entregaRepository, clock);
    }

    private static class DemoDataRunner implements ApplicationRunner {

        private final EmpresaRepository empresaRepository;
        private final EmpresaObrigacaoRepository empresaObrigacaoRepository;
        private final EntregaRepository entregaRepository;
        private final Clock clock;

        private DemoDataRunner(EmpresaRepository empresaRepository,
                               EmpresaObrigacaoRepository empresaObrigacaoRepository,
                               EntregaRepository entregaRepository,
                               Clock clock) {
            this.empresaRepository = empresaRepository;
            this.empresaObrigacaoRepository = empresaObrigacaoRepository;
            this.entregaRepository = entregaRepository;
            this.clock = clock;
        }

        @Override
        @Transactional
        public void run(org.springframework.boot.ApplicationArguments args) {
            if (empresaRepository.count() > 0 || empresaObrigacaoRepository.count() > 0 || entregaRepository.count() > 0) {
                return;
            }
            LocalDate hoje = LocalDate.now(clock);
            LocalDateTime agora = LocalDateTime.now(clock);
            Empresa simples = empresa("Padaria Aurora", "11222333000181", RegimeTributario.SIMPLES);
            Empresa presumido = empresa("Mercado Horizonte", "12345678000195", RegimeTributario.LUCRO_PRESUMIDO);
            Empresa real = empresa("Indústria Vale", "11444777000161", RegimeTributario.LUCRO_REAL);

            vincular(simples, TipoObrigacao.DAS, agora);
            vincular(simples, TipoObrigacao.ECD, agora);
            vincular(presumido, TipoObrigacao.DCTF, agora);
            vincular(presumido, TipoObrigacao.ECD, agora);
            vincular(real, TipoObrigacao.DCTF, agora);

            entregaRepository.save(new Entrega(simples, TipoObrigacao.DAS, YearMonth.now(clock).toString(),
                    hoje.plusDays(20), agora));

            Entrega atrasada = new Entrega(simples, TipoObrigacao.ECD, String.valueOf(Year.now(clock).getValue() - 1),
                    hoje.minusDays(2), agora);
            atrasada.marcarAtrasada(hoje);
            entregaRepository.save(atrasada);

            entregaRepository.save(new Entrega(presumido, TipoObrigacao.DCTF, YearMonth.now(clock).toString(),
                    hoje.plusDays(3), agora));

            Entrega concluida = new Entrega(presumido, TipoObrigacao.ECD, String.valueOf(Year.now(clock).getValue() - 1),
                    hoje.plusDays(10), agora);
            concluida.concluir(hoje, hoje);
            entregaRepository.save(concluida);

            Entrega cancelada = new Entrega(real, TipoObrigacao.DCTF, YearMonth.now(clock).minusMonths(1).toString(),
                    hoje.plusDays(15), agora);
            cancelada.cancelar("Demonstração de entrega cancelada");
            entregaRepository.save(cancelada);
        }

        private Empresa empresa(String nome, String cnpj, RegimeTributario regime) {
            Empresa empresa = new Empresa();
            empresa.setNome(nome);
            empresa.setCnpj(cnpj);
            empresa.setRegimeTributario(regime);
            return empresaRepository.save(empresa);
        }

        private void vincular(Empresa empresa, TipoObrigacao tipo, LocalDateTime criadoEm) {
            EmpresaObrigacao vinculo = new EmpresaObrigacao();
            vinculo.setEmpresa(empresa);
            vinculo.setTipo(tipo);
            vinculo.setCriadoEm(criadoEm);
            empresaObrigacaoRepository.save(vinculo);
        }
    }
}
