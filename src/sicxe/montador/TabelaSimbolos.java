package sicxe.montador;

public class TabelaSimbolos {

    public static class Simbolo {
        public final String nome;
        public final int valor;
        public final boolean relativo;
        public final int linha;         // linha do fonte onde foi definido

        public Simbolo(String nome, int valor, boolean relativo, int linha) {
            this.nome = nome;
            this.valor = valor;
            this.relativo = relativo;
            this.linha = linha;
        }
    }
}