package sicxe.montador;

import java.util.List;
public class Passagem1 {

    private final TabelaSimbolos simbolos;

    private String nomePrograma = "";
    private int enderecoInicial = 0;
    private int tamanhoPrograma = 0;
    private Linha linhaEnd;

    public Passagem1(TabelaSimbolos simbolos) {
        this.simbolos = simbolos;
    }

    public void executar(List<Linha> linhas) {
        int lc = 0;
        int maiorLc = 0;
        int lcAntesDoOrg = -1;
        boolean primeira = true;

        for (Linha linha : linhas) {
            if (linha.vazia || linha.erro != null) {
                continue;
            }
            if (linhaEnd != null) {
                linha.erro = "nada pode vir depois do END";
                continue;
            }
            try {
                String op = linha.operacao;
                if (op.equals("START")) {
                    if (!primeira) {
                        throw new ErroMontagem("o START tem que ser a primeira linha do programa");
                    }
                    lc = lerHexadecimal(linha.operando);
                    maiorLc = lc;
                    enderecoInicial = lc;
                    nomePrograma = linha.rotulo == null ? "" : linha.rotulo;
                }
                primeira = false;
                linha.endereco = lc;

                if (linha.formato4 && !(TabelaInstrucoes.existe(op) && TabelaInstrucoes.formato(op) == 3)) {
                    throw new ErroMontagem("o '+' so vale para instrucoes de formato 3");
                }

                if (op.equals("EQU")) {
                    if (linha.rotulo == null) {
                        throw new ErroMontagem("o EQU precisa de um rotulo");
                    }
                    Expressao.Valor v = Expressao.avaliar(linha.expressao, simbolos, lc);
                    simbolos.definir(linha.rotulo, v.valor, v.relativo, linha.numero);
                    continue;
                }
                if (linha.rotulo != null) {
                    simbolos.definir(linha.rotulo, lc, true, linha.numero);
                }

                switch (op) {
                    case "START":
                    case "BASE":
                    case "NOBASE":
                        break;
                    case "END":
                        linhaEnd = linha;
                        break;
                    case "ORG":
                        if (linha.operando == null) {
                            if (lcAntesDoOrg < 0) {
                                throw new ErroMontagem("ORG sem operando, mas nao houve ORG antes");
                            }
                            lc = lcAntesDoOrg;
                            lcAntesDoOrg = -1;
                        } else {
                            lcAntesDoOrg = lc;
                            lc = Expressao.avaliar(linha.expressao, simbolos, lc).valor;
                        }
                        break;
                    case "BYTE":
                        linha.tamanho = Diretivas.tamanhoByte(linha.operando);
                        break;
                    case "WORD":
                        linha.tamanho = Diretivas.TAMANHO_WORD;
                        break;
                    case "RESB":
                    case "RESW":
                        Expressao.Valor q = Expressao.avaliar(linha.expressao, simbolos, lc);
                        if (q.relativo) {
                            throw new ErroMontagem("a quantidade do " + op + " tem que ser um numero, nao um endereco");
                        }
                        linha.tamanho = Diretivas.tamanhoReserva(op, q.valor);
                        break;
                    default:
                        if (!TabelaInstrucoes.existe(op)) {
                            throw new ErroMontagem("operacao desconhecida: " + op);
                        }
                        if (TabelaInstrucoes.formato(op) == 2) {
                            linha.tamanho = 2;
                        } else {
                            linha.tamanho = linha.formato4 ? 4 : 3;
                        }
                }
                lc += linha.tamanho;
                maiorLc = Math.max(maiorLc, lc);
            } catch (ErroMontagem e) {
                linha.erro = e.getMessage();
            }
        }
        tamanhoPrograma = maiorLc - enderecoInicial;
    }

    // O operando do START e o unico numero em hexadecimal (como no Beck).
    private static int lerHexadecimal(String texto) {
        if (texto == null) {
            return 0;
        }
        try {
            int valor = Integer.parseInt(texto, 16);
            if (valor < 0 || valor > 0xFFFFF) {
                throw new ErroMontagem("endereco do START fora do limite: " + texto);
            }
            return valor;
        } catch (NumberFormatException e) {
            throw new ErroMontagem("o endereco do START tem que estar em hexadecimal: " + texto);
        }
    }

    public String getNomePrograma() {
        return nomePrograma;
    }

    public int getEnderecoInicial() {
        return enderecoInicial;
    }

    public int getTamanhoPrograma() {
        return tamanhoPrograma;
    }

    public Linha getLinhaEnd() {
        return linhaEnd;
    }
}
