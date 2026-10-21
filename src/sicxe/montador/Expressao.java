package sicxe.montador;

public final class Expressao {

    public static class Valor {
        public final int valor;
        public final boolean relativo;

        public Valor(int valor, boolean relativo) {
            this.valor = valor;
            this.relativo = relativo;
        }
    }
}