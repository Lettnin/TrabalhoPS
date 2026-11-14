package sicxe.macro;

import java.util.ArrayList;
import java.util.List;

// Listas de parametros: os formais (na definicao) e os reais (na chamada).
//
//   Definicao:  INCR   MACRO   &VAR,&PASSO=1        &PASSO tem valor padrao 1
//   Chamadas:          INCR    CONTA,5              posicional: &VAR=CONTA, &PASSO=5
//                      INCR    CONTA                omitido:    &PASSO fica com o padrao, 1
//                      INCR    PASSO=5,VAR=CONTA    palavra-chave, em qualquer ordem
//
// O resultado de associar e a "lista de parametros reais" dos slides,
// indexada pela posicao: valores[1] substitui o ?1 do esqueleto, valores[2]
// o ?2, e assim por diante. A posicao 0 e do numero de serie (.SER), que o
// ProcessadorMacros preenche.

public final class Argumentos {

    private Argumentos() { }

    // Separa pelas virgulas, sem quebrar dentro de apostrofos: C'A,B' e um
    // parametro so. "X,,Z" da tres parametros, o do meio vazio.
    public static List<String> separar(String operando) {
        List<String> lista = new ArrayList<>();
        if (operando == null || operando.isEmpty()) {
            return lista;
        }
        StringBuilder atual = new StringBuilder();
        boolean entreApostrofos = false;
        for (char c : operando.toCharArray()) {
            if (c == '\'') {
                entreApostrofos = !entreApostrofos;
            }
            if (c == ',' && !entreApostrofos) {
                lista.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        lista.add(atual.toString());
        return lista;
    }

    // Le os parametros formais do prototipo ("&A,&B,&C=TOTAL") e guarda na macro.
    public static void lerFormais(Macro macro, String operando) {
        for (String parametro : separar(operando)) {
            int igual = parametro.indexOf('=');
            String nome = (igual >= 0 ? parametro.substring(0, igual) : parametro).toUpperCase();
            String padrao = igual >= 0 ? parametro.substring(igual + 1) : "";
            if (!nomeValido(nome)) {
                throw new ErroMacro("parametro formal invalido: '" + parametro
                        + "' (use &NOME, so letras e digitos)");
            }
            macro.adicionarFormal(nome, padrao);
        }
    }

    // Associa os parametros reais da chamada aos formais da macro.
    public static String[] associar(Macro macro, String operando) {
        int quantos = macro.formais.size();
        String[] valores = new String[quantos + 1];
        int proximaPosicao = 1;
        for (String real : separar(operando)) {
            int igual = real.indexOf('=');
            String nome = igual > 0 ? real.substring(0, igual).toUpperCase() : "";
            int posicao;
            if (igual > 0 && nomeValido(nome)) {            // palavra-chave: NOME=valor
                posicao = macro.indiceFormal(nome);
                if (posicao < 0) {
                    throw new ErroMacro("a macro " + macro.nome + " nao tem o parametro " + nome);
                }
                real = real.substring(igual + 1);
            } else {                                        // posicional: vale a ordem
                if (proximaPosicao > quantos) {
                    throw new ErroMacro("parametros demais na chamada de " + macro.nome
                            + " (ela tem " + quantos + ")");
                }
                posicao = proximaPosicao++;
            }
            if (valores[posicao] != null) {
                throw new ErroMacro("o parametro " + macro.formais.get(posicao - 1)
                        + " recebeu valor duas vezes");
            }
            valores[posicao] = real;
        }
        // Parametro omitido (ou vazio) fica com o valor padrao; sem padrao, vazio.
        for (int k = 1; k <= quantos; k++) {
            if (valores[k] == null || valores[k].isEmpty()) {
                valores[k] = macro.padroes.get(k - 1);
            }
        }
        return valores;
    }

    // &NOME ou NOME: letra no comeco, depois letras e digitos.
    private static boolean nomeValido(String nome) {
        String n = nome.startsWith("&") ? nome.substring(1) : nome;
        if (n.isEmpty() || !Character.isLetter(n.charAt(0))) {
            return false;
        }
        for (char c : n.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                return false;
            }
        }
        return true;
    }
}
