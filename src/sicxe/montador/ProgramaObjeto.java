package sicxe.montador;

import java.util.List;

import sicxe.maquina.Palavra;


public class ProgramaObjeto {

    public static final int MAXIMO_POR_REGISTRO = 30;

    public static class Trecho {
        public final int endereco;                  // onde o primeiro byte fica na memoria
        public final int[] bytes;                   // o codigo gerado (vazio = nao gera nada)
        public final int enderecoModificacao;       // onde comeca o campo a corrigir
        public final int meiosBytesModificacao;     // 5 no formato 4, 6 no WORD; 0 = sem registro M

        public Trecho(int endereco, int[] bytes) {
            this(endereco, bytes, -1, 0);
        }

        public Trecho(int endereco, int[] bytes, int enderecoModificacao, int meiosBytesModificacao) {
            this.endereco = endereco;
            this.bytes = bytes;
            this.enderecoModificacao = enderecoModificacao;
            this.meiosBytesModificacao = meiosBytesModificacao;
        }
    }

    private final StringBuilder saida = new StringBuilder();
    private final StringBuilder bytesDoT = new StringBuilder();
    private int inicioDoT;
    private int quantosNoT = 0;

    private ProgramaObjeto() { }

    public static String gerar(String nome, int inicio, int tamanho, int execucao, List<Trecho> trechos) {
        ProgramaObjeto p = new ProgramaObjeto();
        p.saida.append("H^").append(completar(nome, 6)).append('^')
               .append(Palavra.hex(inicio, 6)).append('^').append(Palavra.hex(tamanho, 6)).append('\n');

        int proximo = inicio;           // endereco logo depois do ultimo byte gerado
        for (Trecho trecho : trechos) {
            int[] codigo = trecho.bytes;
            if (codigo.length == 0) {
                continue;
            }
            boolean buraco = trecho.endereco != proximo;
            boolean naoCabe = p.quantosNoT + codigo.length > MAXIMO_POR_REGISTRO;
            if (buraco || naoCabe) {
                p.fecharT();
            }
            for (int k = 0; k < codigo.length; k++) {
                if (p.quantosNoT == MAXIMO_POR_REGISTRO) {
                    p.fecharT();        // so acontece com um BYTE de mais de 30 bytes
                }
                if (p.quantosNoT == 0) {
                    p.inicioDoT = trecho.endereco + k;
                }
                p.bytesDoT.append(Palavra.hex(codigo[k], 2));
                p.quantosNoT++;
            }
            proximo = trecho.endereco + codigo.length;
        }
        p.fecharT();

        for (Trecho trecho : trechos) {
            if (trecho.meiosBytesModificacao > 0) {
                p.saida.append("M^").append(Palavra.hex(trecho.enderecoModificacao - inicio, 6))
                       .append('^').append(Palavra.hex(trecho.meiosBytesModificacao, 2)).append('\n');
            }
        }
        p.saida.append("E^").append(Palavra.hex(execucao, 6)).append('\n');
        return p.saida.toString();
    }

    private void fecharT() {
        if (quantosNoT == 0) {
            return;
        }
        saida.append("T^").append(Palavra.hex(inicioDoT, 6)).append('^')
             .append(Palavra.hex(quantosNoT, 2)).append('^').append(bytesDoT).append('\n');
        bytesDoT.setLength(0);
        quantosNoT = 0;
    }

    private static String completar(String texto, int tamanho) {
        StringBuilder sb = new StringBuilder(texto.length() > tamanho ? texto.substring(0, tamanho) : texto);
        while (sb.length() < tamanho) {
            sb.append(' ');
        }
        return sb.toString();
    }
}