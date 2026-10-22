package sicxe.montador;

import java.util.List;
public class Passagem2 {

    private final TabelaSimbolos simbolos;
    private Integer base = null;        // null = nenhum BASE em vigor
    private int enderecoExecucao;

    public Passagem2(TabelaSimbolos simbolos) {
        this.simbolos = simbolos;
    }

    public void executar(List<Linha> linhas, int enderecoInicial) {
        enderecoExecucao = enderecoInicial;
        for (Linha linha : linhas) {
            if (linha.vazia || linha.erro != null) {
                continue;
            }
            try {
                gerar(linha);
            } catch (ErroMontagem e) {
                linha.erro = e.getMessage();
            }
        }
    }

    private void gerar(Linha linha) {
        String op = linha.operacao;
        switch (op) {
            case "START":
            case "EQU":
            case "ORG":
            case "RESB":
            case "RESW":
                return;
            case "BASE":
                base = valor(linha).valor;
                return;
            case "NOBASE":
                base = null;
                return;
            case "END":
                if (linha.operando != null) {
                    enderecoExecucao = valor(linha).valor;
                }
                return;
            case "BYTE":
                linha.codigo = Diretivas.bytesDeByte(linha.operando);
                return;
            case "WORD":
                Expressao.Valor v = valor(linha);
                linha.codigo = Diretivas.bytesDeWord(v.valor);
                if (v.relativo) {       // WORD com rotulo guarda um endereco: corrigir na relocacao
                    linha.enderecoModificacao = linha.endereco;
                    linha.meiosBytesModificacao = 6;
                }
                return;
            default:
                int opcode = TabelaInstrucoes.opcode(op);
                if (TabelaInstrucoes.formato(op) == 2) {
                    linha.codigo = Codificador.formato2(linha, opcode);
                } else {
                    Expressao.Valor alvo = TabelaInstrucoes.semOperando(op) ? null : valor(linha);
                    linha.codigo = Codificador.formato34(linha, opcode, alvo, base);
                }
        }
    }

    private Expressao.Valor valor(Linha linha) {
        return Expressao.avaliar(linha.expressao, simbolos, linha.endereco);
    }

    public int getEnderecoExecucao() {
        return enderecoExecucao;
    }
}
