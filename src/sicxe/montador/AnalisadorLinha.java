package sicxe.montador;

import java.util.ArrayList;
import java.util.List;

public final class AnalisadorLinha {

    private AnalisadorLinha() { }

    public static Linha analisar(int numero, String texto) {
        Linha linha = new Linha();
        linha.numero = numero;
        linha.texto = texto;
        try {
            preencher(linha);
        } catch (ErroMontagem e) {
            linha.erro = e.getMessage();
        }
        return linha;
    }

    private static void preencher(Linha linha) {
        String texto = linha.texto.stripTrailing();
        String semEspacos = texto.stripLeading();
        if (semEspacos.isEmpty() || semEspacos.startsWith(".")) {
            linha.vazia = true;
            return;
        }

        List<String> palavras = palavras(texto);
        int k = 0;
        boolean comecaNaColuna1 = texto.charAt(0) != ' ' && texto.charAt(0) != '\t';
        if (comecaNaColuna1) {
            linha.rotulo = palavras.get(k++).toUpperCase();
            validarRotulo(linha.rotulo);
        }
        if (k >= palavras.size() || palavras.get(k).startsWith(".")) {
            throw new ErroMontagem("falta a operacao depois do rotulo " + linha.rotulo
                    + " (linha sem rotulo tem que comecar com espaco)");
        }

        String operacao = palavras.get(k++).toUpperCase();
        if (operacao.startsWith("+")) {
            linha.formato4 = true;
            operacao = operacao.substring(1);
        }
        linha.operacao = operacao;

        boolean aceitaOperando = !TabelaInstrucoes.semOperando(operacao) && !operacao.equals("NOBASE");
        if (aceitaOperando && k < palavras.size() && !palavras.get(k).startsWith(".")) {
            linha.operando = maiusculas(palavras.get(k));
        }
        separarModos(linha);
    }

    private static void separarModos(Linha linha) {
        linha.expressao = linha.operando;
        if (linha.operando == null || !TabelaInstrucoes.existe(linha.operacao)
                || TabelaInstrucoes.formato(linha.operacao) != 3) {
            return;
        }
        String e = linha.operando;
        if (e.startsWith("#")) {
            linha.imediato = true;
            e = e.substring(1);
        } else if (e.startsWith("@")) {
            linha.indireto = true;
            e = e.substring(1);
        }
        if (e.endsWith(",X")) {
            linha.indexado = true;
            e = e.substring(0, e.length() - 2);
        }
        if (e.isEmpty()) {
            throw new ErroMontagem("operando vazio: " + linha.operando);
        }
        linha.expressao = e;
    }

    private static List<String> palavras(String texto) {
        List<String> lista = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean entreApostrofos = false;
        for (char c : texto.toCharArray()) {
            if (c == '\'') {
                entreApostrofos = !entreApostrofos;
            }
            if (!entreApostrofos && (c == ' ' || c == '\t')) {
                if (atual.length() > 0) {
                    lista.add(atual.toString());
                    atual.setLength(0);
                }
            } else {
                atual.append(c);
            }
        }
        if (atual.length() > 0) {
            lista.add(atual.toString());
        }
        return lista;
    }

    private static String maiusculas(String operando) {
        if (operando.length() >= 2 && (operando.charAt(0) == 'C' || operando.charAt(0) == 'c')
                && operando.charAt(1) == '\'') {
            return "C" + operando.substring(1);
        }
        return operando.toUpperCase();
    }

    private static void validarRotulo(String rotulo) {
        boolean valido = Character.isLetter(rotulo.charAt(0));
        for (char c : rotulo.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                valido = false;
            }
        }
        if (!valido) {
            throw new ErroMontagem("rotulo invalido: " + rotulo + " (use so letras e digitos, comecando por letra)");
        }
    }
}