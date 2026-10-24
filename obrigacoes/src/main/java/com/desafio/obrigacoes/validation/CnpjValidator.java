package com.desafio.obrigacoes.validation;

import io.github.johnpitter.cnpj.CNPJ;
import java.util.regex.Pattern;

public final class CnpjValidator {

    private static final Pattern CARACTERES_PERMITIDOS = Pattern.compile("[A-Za-z0-9./-]+");

    private CnpjValidator() {
    }

    public static boolean isValido(String cnpj) {
        if (cnpj == null) {
            return false;
        }

        String semEspacosNasPontas = cnpj.trim();

        return CARACTERES_PERMITIDOS.matcher(semEspacosNasPontas).matches()
                && CNPJ.isValid(semEspacosNasPontas);
    }

    public static String normalizar(String cnpj) {
        return cnpj == null ? null : CNPJ.unformat(cnpj);
    }
}
