package sicxe.montador;

import java.util.List;

import sicxe.maquina.Palavra;

// A listagem da montagem, cada linha do fonte com o endereco e o codigo
// objeto gerado, o erro logo abaixo da linha que o causou e, no fim, a
// tabela de simbolos. E a linguagem de maquina anotada dos slides.

public final class Listagem {

    private Listagem() { }

    public static String gerar(List<Linha> linhas, TabelaSimbolos simbolos) {
        StringBuilder sb = new StringBuilder();
        sb.append("Linha  Ender.  Codigo objeto   Fonte\n");
        for (Linha linha : linhas) {
            String endereco = linha.endereco >= 0 ? Palavra.hex(linha.endereco, 6) : "";
            if ("EQU".equals(linha.operacao) && linha.erro == null) {
                // no EQU a coluna mostra o valor do simbolo, como no Beck
                endereco = Palavra.hex(simbolos.buscar(linha.rotulo).valor, 6);
            }
            sb.append(String.format("%5d  %-6s  %-14s  %s\n",
                    linha.numero, endereco, linha.codigoEmHex(), linha.texto));
            if (linha.erro != null) {
                sb.append("       *** ERRO: ").append(linha.erro).append('\n');
            }
        }

        sb.append("\nTabela de simbolos\n");
        sb.append("Simbolo     Valor   Tipo\n");
        for (TabelaSimbolos.Simbolo s : simbolos.todos()) {
            sb.append(String.format("%-10s  %s  %s\n",
                    s.nome, Palavra.hex(s.valor, 6), s.relativo ? "relativo" : "absoluto"));
        }
        return sb.toString();
    }
}
