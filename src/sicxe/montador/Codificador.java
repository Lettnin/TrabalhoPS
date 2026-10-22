package sicxe.montador;

import sicxe.cpu.TabelaOpcodes;
import sicxe.maquina.ErroExecucao;
import sicxe.maquina.Palavra;
import sicxe.maquina.Registradores;

// Transforma uma instrucao ja analisada nos bytes de maquina.
// E o caminho inverso do Decodificador e do Enderecamento da Etapa 1:
// la os bytes viravam endereco; aqui o endereco vira bytes.
//
//   formato 2:  opcode (8 bits) | r1 (4) | r2 (4)
//   formato 3:  opcode (6 bits) | n i x b p e | disp (12)
//   formato 4:  opcode (6 bits) | n i x b p e | endereco (20)
//
// Como os bits b e p sao escolhidos no formato 3 (Beck, secao 2.2.1):
//   1. operando absoluto (numero, ou simbolo de EQU com numero):
//        o valor vai direto no disp, com b = p = 0. Tem que caber em 12 bits.
//   2. operando relativo (rotulo do programa):
//        primeiro tenta relativo ao PC: disp = alvo - (endereco + 3), de -2048 a 2047;
//        senao, base-relativo: disp = alvo - BASE, de 0 a 4095, se houver BASE;
//        senao, erro: o programador tem que usar o formato 4 (+).
//   O PC vem primeiro porque so depende do proprio programa. A base so
//   funciona se o programa carregar no registrador B o valor do BASE.

public final class Codificador {

    private Codificador() { }

    // ADDR S,X -> 90 41      CLEAR L -> B4 20      SHIFTL A,4 -> A4 03
    public static int[] formato2(Linha linha, int opcode) {
        String[] partes = linha.operando == null ? new String[0] : linha.operando.split(",");
        int r1;
        int r2 = 0;

        if (opcode == TabelaOpcodes.CLEAR || opcode == TabelaOpcodes.TIXR) {
            exigir(partes, 1, "um registrador");
            r1 = registrador(partes[0]);
        } else if (opcode == TabelaOpcodes.SHIFTL || opcode == TabelaOpcodes.SHIFTR) {
            exigir(partes, 2, "registrador e numero de bits");
            r1 = registrador(partes[0]);
            int bits = numero(partes[1]);
            if (bits < 1 || bits > 16) {
                throw new ErroMontagem("o deslocamento vai de 1 a 16 bits: " + bits);
            }
            r2 = bits - 1;      // a maquina guarda n - 1 (Etapa 1, InstrucoesRegistrador)
        } else {
            exigir(partes, 2, "dois registradores");
            r1 = registrador(partes[0]);
            r2 = registrador(partes[1]);
        }
        return new int[] { opcode, (r1 << 4) | r2 };
    }

    public static int[] formato34(Linha linha, int opcode, Expressao.Valor alvo, Integer base) {
        if (linha.indexado && (linha.imediato || linha.indireto)) {
            throw new ErroMontagem("indexado (,X) nao combina com # nem com @");
        }
        boolean n = !linha.imediato;    // n i = 1 1 simples, 0 1 imediato, 1 0 indireto
        boolean i = !linha.indireto;
        boolean x = linha.indexado;
        boolean b = false;
        boolean p = false;
        boolean e = linha.formato4;
        int campo;                      // o disp (12 bits) ou o endereco (20 bits)

        if (alvo == null) {             // RSUB: nao tem operando
            campo = 0;
        } else if (e) {
            if (alvo.valor < 0 || alvo.valor > 0xFFFFF) {
                throw new ErroMontagem("valor nao cabe nos 20 bits do formato 4: " + alvo.valor);
            }
            campo = alvo.valor;
            if (alvo.relativo) {        // endereco do programa: corrigir se ele mudar de lugar
                linha.enderecoModificacao = linha.endereco + 1;
                linha.meiosBytesModificacao = 5;
            }
        } else if (!alvo.relativo) {    // numero fixo: vai direto no disp
            if (alvo.valor < 0 || alvo.valor > 4095) {
                throw new ErroMontagem("o valor " + alvo.valor
                        + " nao cabe nos 12 bits do formato 3; use o formato 4 (+)");
            }
            campo = alvo.valor;
        } else {
            int relativoAoPC = alvo.valor - (linha.endereco + 3);
            if (relativoAoPC >= -2048 && relativoAoPC <= 2047) {
                p = true;
                campo = relativoAoPC & 0xFFF;   // complemento de 2 em 12 bits
            } else if (base != null && alvo.valor - base >= 0 && alvo.valor - base <= 4095) {
                b = true;
                campo = alvo.valor - base;
            } else {
                throw new ErroMontagem("o endereco " + Palavra.hex(alvo.valor, 6)
                        + " esta longe demais para o PC" + (base == null ? " (e nao ha BASE)" : " e para a BASE")
                        + "; use o formato 4 (+)");
            }
        }

        int primeiroByte = opcode | (n ? 2 : 0) | (i ? 1 : 0);
        int bits = (x ? 8 : 0) | (b ? 4 : 0) | (p ? 2 : 0) | (e ? 1 : 0);
        if (e) {
            return new int[] { primeiroByte, (bits << 4) | (campo >> 16), (campo >> 8) & 0xFF, campo & 0xFF };
        }
        return new int[] { primeiroByte, (bits << 4) | (campo >> 8), campo & 0xFF };
    }

    private static void exigir(String[] partes, int quantos, String descricao) {
        if (partes.length != quantos) {
            throw new ErroMontagem("esta instrucao precisa de " + descricao);
        }
    }

    // Reaproveita a tabela de nomes dos Registradores da Etapa 1.
    private static int registrador(String nome) {
        try {
            return Registradores.numeroPorNome(nome.trim());
        } catch (ErroExecucao e) {
            throw new ErroMontagem("registrador desconhecido: " + nome);
        }
    }

    private static int numero(String texto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new ErroMontagem("numero invalido: " + texto);
        }
    }
}
