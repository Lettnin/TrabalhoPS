package sicxe.montador;

// Numeros decimais, simbolos e LC, com sinal inicial opcional.
// Multiplicacao e divisao de absolutos precedem soma e subtracao.
// O saldo relativo so e validado depois de avaliar toda a expressao.
public final class Expressao {

    public static class Valor {
        public final int valor;
        public final boolean relativo;

        public Valor(int valor, boolean relativo) {
            this.valor = valor;
            this.relativo = relativo;
        }
    }

    private Expressao() { }

    public static Valor avaliar(String texto, TabelaSimbolos simbolos, int lc) {
        if (texto == null || texto.isEmpty()) {
            throw new ErroMontagem("falta o operando");
        }
        int total = 0;
        int relativos = 0;
        int sinal = 1;
        int inicio = 0;
        if (texto.charAt(0) == '-' || texto.charAt(0) == '+') {
            sinal = texto.charAt(0) == '-' ? -1 : 1;
            inicio = 1;
        }

        while (true) {
            int fim = inicio;
            while (fim < texto.length() && texto.charAt(fim) != '+' && texto.charAt(fim) != '-') {
                fim++;
            }
            Valor termo = produto(texto.substring(inicio, fim), simbolos, lc, texto);
            total += sinal * termo.valor;
            if (termo.relativo) {
                relativos += sinal;
            }
            if (fim >= texto.length()) {
                break;
            }
            sinal = texto.charAt(fim) == '+' ? 1 : -1;
            inicio = fim + 1;
        }

        if (relativos != 0 && relativos != 1) {
            throw new ErroMontagem("expressao sem sentido: " + texto
                    + " (so da para somar um endereco do programa)");
        }
        return new Valor(total, relativos == 1);
    }

    // Resolve a cadeia de * e / da esquerda para a direita, antes da soma.
    private static Valor produto(String trecho, TabelaSimbolos simbolos, int lc, String texto) {
        int inicio = 0;
        char operador = 0;
        Valor resultado = null;
        while (true) {
            int fim = inicio;
            // Aqui se espera um termo: o primeiro '*' pertence ao LC.
            if (fim < trecho.length() && trecho.charAt(fim) == '*') {
                fim++;
            }
            while (fim < trecho.length() && trecho.charAt(fim) != '*' && trecho.charAt(fim) != '/') {
                fim++;
            }
            Valor fator = termo(trecho.substring(inicio, fim), simbolos, lc);
            if (resultado == null) {
                resultado = fator;
            } else {
                if (resultado.relativo || fator.relativo) {
                    String operacao = operador == '*' ? "multiplicacao" : "divisao";
                    throw new ErroMontagem(operacao + " com valor relativo na expressao: " + texto);
                }
                if (operador == '/' && fator.valor == 0) {
                    throw new ErroMontagem("divisao por zero na expressao: " + texto);
                }
                int valor = operador == '*' ? resultado.valor * fator.valor : resultado.valor / fator.valor;
                resultado = new Valor(valor, false);
            }
            if (fim >= trecho.length()) {
                return resultado;
            }
            // Depois de um termo completo, '*' e multiplicacao.
            operador = trecho.charAt(fim);
            inicio = fim + 1;
        }
    }

    private static Valor termo(String termo, TabelaSimbolos simbolos, int lc) {
        if (termo.isEmpty()) {
            throw new ErroMontagem("expressao incompleta");
        }
        if (termo.equals("*")) {
            return new Valor(lc, true);
        }
        if (Character.isDigit(termo.charAt(0))) {
            try {
                return new Valor(Integer.parseInt(termo), false);
            } catch (NumberFormatException e) {
                throw new ErroMontagem("numero invalido: " + termo);
            }
        }
        TabelaSimbolos.Simbolo s = simbolos.buscar(termo);
        return new Valor(s.valor, s.relativo);
    }
}
