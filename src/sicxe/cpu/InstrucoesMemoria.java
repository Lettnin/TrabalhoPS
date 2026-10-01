package sicxe.cpu;

import sicxe.maquina.ErroExecucao;
import sicxe.maquina.Memoria;
import sicxe.maquina.Palavra;
import sicxe.maquina.Registradores;

public class InstrucoesMemoria {

    private final Memoria mem;
    private final Registradores regs;
    private final Enderecamento ender;

    public InstrucoesMemoria(Memoria mem, Registradores regs, Enderecamento ender) {
        this.mem = mem;
        this.regs = regs;
        this.ender = ender;
    }

    public Resultado executar(Instrucao ins) {
        switch (ins.opcode) {

            // Carga.
            case TabelaOpcodes.LDA:
                regs.set(Registradores.A, ender.valorPalavra(ins));
                return Resultado.CONTINUA;
            case TabelaOpcodes.LDX:
                regs.set(Registradores.X, ender.valorPalavra(ins));
                return Resultado.CONTINUA;
            case TabelaOpcodes.LDL:
                regs.set(Registradores.L, ender.valorPalavra(ins));
                return Resultado.CONTINUA;
            case TabelaOpcodes.LDB:
                regs.set(Registradores.B, ender.valorPalavra(ins));
                return Resultado.CONTINUA;
            case TabelaOpcodes.LDS:
                regs.set(Registradores.S, ender.valorPalavra(ins));
                return Resultado.CONTINUA;
            case TabelaOpcodes.LDT:
                regs.set(Registradores.T, ender.valorPalavra(ins));
                return Resultado.CONTINUA;
            case TabelaOpcodes.LDCH: {
                // Preserva os 16 bits superiores de A.
                int byteLido = ender.valorByte(ins);
                int novoA = (regs.get(Registradores.A) & 0xFFFF00) | byteLido;
                regs.set(Registradores.A, novoA);
                return Resultado.CONTINUA;
            }

            // Armazenamento.
            case TabelaOpcodes.STA:
                mem.escreverPalavra(ender.enderecoDestino(ins), regs.get(Registradores.A));
                return Resultado.CONTINUA;
            case TabelaOpcodes.STX:
                mem.escreverPalavra(ender.enderecoDestino(ins), regs.get(Registradores.X));
                return Resultado.CONTINUA;
            case TabelaOpcodes.STL:
                mem.escreverPalavra(ender.enderecoDestino(ins), regs.get(Registradores.L));
                return Resultado.CONTINUA;
            case TabelaOpcodes.STB:
                mem.escreverPalavra(ender.enderecoDestino(ins), regs.get(Registradores.B));
                return Resultado.CONTINUA;
            case TabelaOpcodes.STS:
                mem.escreverPalavra(ender.enderecoDestino(ins), regs.get(Registradores.S));
                return Resultado.CONTINUA;
            case TabelaOpcodes.STT:
                mem.escreverPalavra(ender.enderecoDestino(ins), regs.get(Registradores.T));
                return Resultado.CONTINUA;
            case TabelaOpcodes.STCH:
                mem.escreverByte(ender.enderecoDestino(ins), regs.get(Registradores.A) & 0xFF);
                return Resultado.CONTINUA;

            // Aritmetica com sinal; regs.set aplica a mascara de 24 bits.
            case TabelaOpcodes.ADD:
                regs.set(Registradores.A, comSinal(Registradores.A) + comSinal(ins));
                return Resultado.CONTINUA;
            case TabelaOpcodes.SUB:
                regs.set(Registradores.A, comSinal(Registradores.A) - comSinal(ins));
                return Resultado.CONTINUA;
            case TabelaOpcodes.MUL:
                regs.set(Registradores.A, comSinal(Registradores.A) * comSinal(ins));
                return Resultado.CONTINUA;
            case TabelaOpcodes.DIV: {
                int divisor = comSinal(ins);
                if (divisor == 0) {
                    throw new ErroExecucao("Divisao por zero no endereco "
                            + Palavra.hex(ins.endereco, 6) + ".");
                }
                regs.set(Registradores.A, comSinal(Registradores.A) / divisor);
                return Resultado.CONTINUA;
            }

            // Logica sobre os bits da palavra.
            case TabelaOpcodes.AND:
                regs.set(Registradores.A, regs.get(Registradores.A) & ender.valorPalavra(ins));
                return Resultado.CONTINUA;
            case TabelaOpcodes.OR:
                regs.set(Registradores.A, regs.get(Registradores.A) | ender.valorPalavra(ins));
                return Resultado.CONTINUA;

            // Comparacao e TIX (incrementa X antes de comparar).
            case TabelaOpcodes.COMP:
                regs.comparar(regs.get(Registradores.A), ender.valorPalavra(ins));
                return Resultado.CONTINUA;
            case TabelaOpcodes.TIX: {
                int x = Palavra.mascarar(regs.get(Registradores.X) + 1);
                regs.set(Registradores.X, x);
                regs.comparar(x, ender.valorPalavra(ins));
                return Resultado.CONTINUA;
            }

            default:
                return Resultado.NAO_TRATADA;
        }
    }

    private int comSinal(Instrucao ins) {
        return Palavra.paraSinalizado(ender.valorPalavra(ins));
    }

    private int comSinal(int registrador) {
        return Palavra.paraSinalizado(regs.get(registrador));
    }
}
