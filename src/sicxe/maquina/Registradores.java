package sicxe.maquina;

// Banco de registradores do SIC/XE.

// Os numeros dos registradores sao os definidos na especificacao e sao
// usados pelas instrucoes de formato 2 (ADDR, RMO, CLEAR, ...)
// O registrador 7 nao existe. O F (ponto flutuante, 48 bits) existe na
// arquitetura mas nao e usado neste trabalho, porque todas as instrucoes
// de ponto flutuante estao em vermelho no enunciado.

// O SW guarda o codigo de condicao (CC) nos 2 bits menos significativos.

public class Registradores {

    public static final int A = 0;
    public static final int X = 1;
    public static final int L = 2;
    public static final int B = 3;
    public static final int S = 4;
    public static final int T = 5;
    public static final int F = 6;
    public static final int PC = 8;
    public static final int SW = 9;

    public static final int CC_IGUAL = 0;
    public static final int CC_MENOR = 1;
    public static final int CC_MAIOR = 2;

    private static final String[] NOMES = { "A", "X", "L", "B", "S", "T", "F", "?", "PC", "SW" };

    private final int[] valores = new int[10];

    private void validar(int numero) {
        if (numero < 0 || numero > 9 || numero == 7) {
            throw new ErroExecucao("Registrador inexistente: " + numero);
        }
    }

    public int get(int numero) {
        validar(numero);
        return valores[numero];
    }

    public void set(int numero, int valor) {
        validar(numero);
        valores[numero] = Palavra.mascarar(valor);
    }

    public int getPC() {
        return valores[PC];
    }

    public void setPC(int valor) {
        valores[PC] = Palavra.mascarar(valor);
    }

    public int getCC() {
        return valores[SW] & 0x03;
    }

    public void setCC(int cc) {
        valores[SW] = (valores[SW] & ~0x03) | (cc & 0x03);
    }

    // Compara dois valores de 24 bits COM SINAL e atualiza o CC.
    public void comparar(int valor1, int valor2) {
        int a = Palavra.paraSinalizado(valor1);
        int b = Palavra.paraSinalizado(valor2);
        if (a == b) {
            setCC(CC_IGUAL);
        } else if (a < b) {
            setCC(CC_MENOR);
        } else {
            setCC(CC_MAIOR);
        }
    }

    public String getCCTexto() {
        switch (getCC()) {
            case CC_IGUAL: return "=";
            case CC_MENOR: return "<";
            case CC_MAIOR: return ">";
            default:       return "?";
        }
    }

    public static String nome(int numero) {
        if (numero < 0 || numero >= NOMES.length) {
            return "R" + numero;
        }
        return NOMES[numero];
    }

    public static int numeroPorNome(String nome) {
        for (int i = 0; i < NOMES.length; i++) {
            // a posicao 7 nao e registrador, o "?" so ocupa o lugar no vetor
            if (i != 7 && NOMES[i].equalsIgnoreCase(nome)) {
                return i;
            }
        }
        throw new ErroExecucao("Registrador desconhecido: " + nome);
    }

    public void limpar() {
        for (int i = 0; i < valores.length; i++) {
            valores[i] = 0;
        }
    }
}
