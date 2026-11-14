package sicxe.macro;

import java.util.List;

// As duas trocas de texto do processador de macros.
//
// 1) Na DEFINICAO (preparar): cada parametro formal do corpo vira a posicao
//    dele no prototipo. E o "pre-tratamento dos parametros formais" dos
//    slides, que deixa a expansao simples:
//        LDA     &A          ->   LDA     ?1
//        J       FIM.SER     ->   J       FIM?0       (o .SER e o parametro 0)
//
// 2) Na EXPANSAO (expandir): cada ?n vira o parametro real numero n.
//        LDA     ?1          ->   LDA     PRECO       (chamada SOMA PRECO,...)
//        J       FIM?0       ->   J       FIM0003     (3a expansao do programa)
//
// A troca e por palavra inteira: com o formal &A, o texto &AB nao muda.
// Aqui, palavra e uma sequencia de letras, digitos e '&'.

public final class Substituicao {

    private Substituicao() { }

    // A pilha tem os formais de cada definicao aberta. pilha.get(0) e a
    // macro que esta sendo guardada; as seguintes sao macros definidas
    // dentro dela (definicoes aninhadas). Cada lista tem ".SER" na posicao 0.
    //
    // A palavra e procurada do nivel mais de dentro para o mais de fora. So
    // vira ?n se for formal da macro de fora (nivel 0). Se for formal de uma
    // macro de dentro, fica com o nome e so vira ?n quando a macro de dentro
    // for definida, durante a expansao da de fora. E o par #(d,i) dos
    // slides: d e o nivel onde o nome foi declarado, i a posicao.
    public static String preparar(String texto, List<List<String>> pilha) {
        StringBuilder saida = new StringBuilder();
        int i = 0;
        while (i < texto.length()) {
            int fim = fimDaPalavra(texto, i);
            if (fim == i) {                     // nao comeca palavra aqui: copia o caractere
                saida.append(texto.charAt(i));
                i++;
                continue;
            }
            String palavra = texto.substring(i, fim);
            int posicao = posicaoNaMacroDeFora(palavra.toUpperCase(), pilha);
            saida.append(posicao >= 0 ? "?" + posicao : palavra);
            i = fim;
        }
        return saida.toString();
    }

    // valores[n] e o texto que entra no lugar de ?n.
    public static String expandir(String linha, String[] valores) {
        StringBuilder saida = new StringBuilder();
        int i = 0;
        while (i < linha.length()) {
            int fim = i + 1;
            while (fim < linha.length() && Character.isDigit(linha.charAt(fim))) {
                fim++;
            }
            if (linha.charAt(i) == '?' && fim > i + 1) {
                int n = Integer.parseInt(linha.substring(i + 1, fim));
                saida.append(n < valores.length ? valores[n] : "");
                i = fim;
            } else {
                saida.append(linha.charAt(i));
                i++;
            }
        }
        return saida.toString();
    }

    private static int posicaoNaMacroDeFora(String palavra, List<List<String>> pilha) {
        for (int nivel = pilha.size() - 1; nivel >= 0; nivel--) {
            int k = pilha.get(nivel).indexOf(palavra);
            if (k >= 0) {
                return nivel == 0 ? k : -1;
            }
        }
        return -1;
    }

    // Onde termina a palavra que comeca em i (devolve i se nao comeca
    // palavra ali). ".SER" conta como uma palavra: "FIM.SER" e FIM + .SER.
    private static int fimDaPalavra(String texto, int i) {
        if (texto.regionMatches(true, i, ".SER", 0, 4)
                && (i + 4 == texto.length() || !ehDaPalavra(texto.charAt(i + 4)))) {
            return i + 4;
        }
        int fim = i;
        while (fim < texto.length() && ehDaPalavra(texto.charAt(fim))) {
            fim++;
        }
        return fim;
    }

    private static boolean ehDaPalavra(char c) {
        return Character.isLetterOrDigit(c) || c == '&';
    }
}
