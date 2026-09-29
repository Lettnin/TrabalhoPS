package sicxe.maquina;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// Memoria principal da maquina SIC/XE.
 
// E um vetor de bytes. Cada posicao guarda 8 bits (0..255).
// Uma PALAVRA ocupa 3 bytes consecutivos (24 bits), armazenados
// com o byte mais significativo primeiro (big-endian), exatamente
// como o livro do Beck descreve.
 
// A classe tambem anota quais enderecos foram escritos no passo atual,
// para a interface grafica poder destacar o que mudou.
 
public class Memoria {

    public static final int TAMANHO_PADRAO = 32768;  // 32 KB
    public static final int BYTES_POR_PALAVRA = 3;

    private final byte[] celulas;
    private final Set<Integer> escritasRecentes = new HashSet<>();

    public Memoria() {
        this(TAMANHO_PADRAO);
    }

    public Memoria(int tamanho) {
        if (tamanho < 1024) {
            throw new ErroExecucao("A memoria nao pode ser menor que 1 KB.");
        }
        this.celulas = new byte[tamanho];
    }

    public int getTamanho() {
        return celulas.length;
    }

    private void validar(int endereco) {
        if (endereco < 0 || endereco >= celulas.length) {
            throw new ErroExecucao("Endereco fora da memoria: " + Palavra.hex(endereco, 6));
        }
    }

    // Le um byte (0..255). O "& 0xFF" existe porque byte em Java vai de -128 a 127.
    public int lerByte(int endereco) {
        validar(endereco);
        return celulas[endereco] & 0xFF;
    }

    public void escreverByte(int endereco, int valor) {
        validar(endereco);
        celulas[endereco] = (byte) (valor & 0xFF);
        escritasRecentes.add(endereco);
    }

    // Le 3 bytes consecutivos e monta a palavra de 24 bits.
    public int lerPalavra(int endereco) {
        return (lerByte(endereco) << 16)
             | (lerByte(endereco + 1) << 8)
             |  lerByte(endereco + 2);
    }

    public void escreverPalavra(int endereco, int valor) {
        escreverByte(endereco,     (valor >> 16) & 0xFF);
        escreverByte(endereco + 1, (valor >> 8)  & 0xFF);
        escreverByte(endereco + 2,  valor        & 0xFF);
    }

    public void limpar() {
        Arrays.fill(celulas, (byte) 0);
        escritasRecentes.clear();
    }

    public void limparEscritasRecentes() {
        escritasRecentes.clear();
    }

    public boolean foiEscritoRecentemente(int endereco) {
        return escritasRecentes.contains(endereco);
    }
}
