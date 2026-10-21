package sicxe.montador;

import java.util.List;
import sicxe.maquina.Palavra;

public class ProgramaObjeto {

    public static final int MAXIMO_POR_REGISTRO = 30;

    private final StringBuilder saida = new StringBuilder();
    private final StringBuilder bytesDoT = new StringBuilder();
    private int inicioDoT;
    private int quantosNoT = 0;

    private ProgramaObjeto() { }

    public static String gerar(String nome, int inicio, int tamanho, int execucao, List<Linha> linhas) {
        ProgramaObjeto p = new ProgramaObjeto();
        p.saida.append("H^").append(completar(nome, 6)).append('^')
               .append(Palavra.hex(inicio, 6)).append('^').append(Palavra.hex(tamanho, 6)).append('\n');

        int proximo = inicio;           // endereco logo depois do ultimo byte gerado
        for (Linha linha : linhas) {
            int[] codigo = linha.codigo;
            if (codigo.length == 0) {
                continue;
            }
            boolean buraco = linha.endereco != proximo;
            boolean naoCabe = p.quantosNoT + codigo.length > MAXIMO_POR_REGISTRO;
            if (buraco || naoCabe) {
                p.fecharT();
            }
            for (int k = 0; k < codigo.length; k++) {
                if (p.quantosNoT == MAXIMO_POR_REGISTRO) {
                    p.fecharT();        // so acontece com um BYTE de mais de 30 bytes
                }
                if (p.quantosNoT == 0) {
                    p.inicioDoT = linha.endereco + k;
                }
                p.bytesDoT.append(Palavra.hex(codigo[k], 2));
                p.quantosNoT++;
            }
            proximo = linha.endereco + codigo.length;
        }
        p.fecharT();

        for (Linha linha : linhas) {
            if (linha.meiosBytesModificacao > 0) {
                p.saida.append("M^").append(Palavra.hex(linha.enderecoModificacao - inicio, 6))
                       .append('^').append(Palavra.hex(linha.meiosBytesModificacao, 2)).append('\n');
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
