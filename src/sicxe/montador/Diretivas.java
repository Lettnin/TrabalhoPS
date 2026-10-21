package sicxe.montador;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import sicxe.maquina.Palavra;

// Tabela de pseudo-instrucoes (POT) e traducao das constantes.
//
// Uma diretiva nao vira instrucao de maquina, ela da uma ordem ao montador.
//   START n   endereco inicial do programa (em HEXADECIMAL, como no Beck)
//   END   r   fim do fonte; r e onde a execucao comeca
//   BYTE  c   constante de bytes: C'ABC' (caracteres) ou X'F1' (hexadecimal)
//   WORD  n   constante de uma palavra (3 bytes), com sinal
//   RESB  n   reserva n bytes           
//   RESW  n   reserva n palavras
//   EQU   e   define um simbolo com o valor de uma expressao
//   ORG   e   muda o contador de posicao 
//   BASE  e   avisa qual valor o registrador B vai ter durante a execucao
//   NOBASE    desliga o modo base-relativo

public final class Diretivas {

    public static final Set<String> NOMES = new HashSet<>(Arrays.asList(
            "START", "END", "BYTE", "WORD", "RESB", "RESW", "EQU", "ORG", "BASE", "NOBASE"));

    public static final int TAMANHO_WORD = 3;

    // O SIC/XE enderece no maximo 1 MB 
    // Reservar mais que isso nao faz sentido, e sem esse limite RESW 715827883 estoura o int do
    // Java 3 x n fica negativo e o montador gera um programa errado sem avisar.
    public static final int MEMORIA_MAXIMA = 0x100000;

    private Diretivas() { }

    public static boolean ehDiretiva(String operacao) {
        return NOMES.contains(operacao);
    }

    // Quantos bytes a constante de um BYTE ocupa.
    public static int tamanhoByte(String operando) {
        return bytesDeByte(operando).length;
    }

    public static int[] bytesDeByte(String operando) {
        if (operando == null || operando.length() < 4
                || operando.charAt(1) != '\'' || !operando.endsWith("'")) {
            throw new ErroMontagem("constante invalida no BYTE: use C'texto' ou X'hexadecimal'");
        }
        char tipo = operando.charAt(0);
        String conteudo = operando.substring(2, operando.length() - 1);

        if (tipo == 'C') {
            int[] bytes = new int[conteudo.length()];
            for (int k = 0; k < conteudo.length(); k++) {
                char c = conteudo.charAt(k);
                if (c > 127) {
                    throw new ErroMontagem("use so caracteres sem acento em C'...'");
                }
                bytes[k] = c;
            }
            return bytes;
        }
        if (tipo == 'X') {
            if (conteudo.length() % 2 != 0) {
                throw new ErroMontagem("X'...' precisa de um numero par de digitos hexadecimais");
            }
            int[] bytes = new int[conteudo.length() / 2];
            for (int k = 0; k < bytes.length; k++) {
                int alto = Character.digit(conteudo.charAt(2 * k), 16);
                int baixo = Character.digit(conteudo.charAt(2 * k + 1), 16);
                if (alto < 0 || baixo < 0) {
                    throw new ErroMontagem("digito hexadecimal invalido no BYTE: " + conteudo);
                }
                bytes[k] = alto * 16 + baixo;
            }
            return bytes;
        }
        throw new ErroMontagem("tipo de constante desconhecido no BYTE: " + tipo);
    }

    // Uma palavra de 3 bytes, o mais significativo primeiro
    // Numero negativo vira complemento de 2: -15 -> FF FF F1
    public static int[] bytesDeWord(int valor) {
        if (valor < -0x800000 || valor > 0xFFFFFF) {
            throw new ErroMontagem("valor nao cabe em 24 bits: " + valor);
        }
        int v = Palavra.mascarar(valor);
        return new int[] { (v >> 16) & 0xFF, (v >> 8) & 0xFF, v & 0xFF };
    }

    // RESB n reserva n bytes; RESW n reserva n palavras (3n bytes).
    public static int tamanhoReserva(String operacao, int quantidade) {
        if (quantidade < 0) {
            throw new ErroMontagem("quantidade negativa no " + operacao);
        }
        // long, para a conta 3 x n nao estourar antes de comparar com o limite
        long bytes = operacao.equals("RESW") ? (long) quantidade * TAMANHO_WORD : quantidade;
        if (bytes > MEMORIA_MAXIMA) {
            throw new ErroMontagem("reserva grande demais no " + operacao + ": " + quantidade
                    + " (o SIC/XE enderece no maximo 1 MB)");
        }
        return (int) bytes;
    }
}
