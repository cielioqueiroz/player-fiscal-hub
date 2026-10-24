package com.desafio.obrigacoes.validation;

import io.github.johnpitter.cnpj.CNPJ;
import java.util.regex.Pattern;

public final class CnpjValidator {

    private static final Pattern SEM_MASCARA = Pattern.compile("[A-Za-z0-9]{12}[0-9]{2}");
    private static final Pattern COM_MASCARA = Pattern.compile("[A-Za-z0-9]{2}\\.[A-Za-z0-9]{3}\\.[A-Za-z0-9]{3}/[A-Za-z0-9]{4}-[0-9]{2}");

    private CnpjValidator() {
    }

    public static boolean isValido(String cnpj) {
        if (cnpj == null) {
            return false;
        }

        String semEspacosNasPontas = cnpj.trim();

        return (SEM_MASCARA.matcher(semEspacosNasPontas).matches()
                || COM_MASCARA.matcher(semEspacosNasPontas).matches())
                && CNPJ.isValid(semEspacosNasPontas);
    }

    public static String normalizar(String cnpj) {
        return cnpj == null ? null : CNPJ.unformat(cnpj);
    }
}
