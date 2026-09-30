package sicxe.cpu;

import sicxe.maquina.ErroExecucao;
import sicxe.maquina.Palavra;
import sicxe.maquina.Registradores;

public class InstrucoesRegistrador {

    private final Registradores regs;
    private final Enderecamento ender;

    public InstrucoesRegistrador(Registradores regs, Enderecamento ender) {
        this.regs = regs;
        this.ender = ender;
    }

    public Resultado executar(Instrucao ins) {
        switch (ins.opcode) {

            case TabelaOpcodes.ADDR:
                regs.set(ins.r2, sinal(ins.r2) + sinal(ins.r1));
                return Resultado.CONTINUA;
            case TabelaOpcodes.SUBR:
                regs.set(ins.r2, sinal(ins.r2) - sinal(ins.r1));
                return Resultado.CONTINUA;
            case TabelaOpcodes.MULR:
                regs.set(ins.r2, sinal(ins.r2) * sinal(ins.r1));
                return Resultado.CONTINUA;
            case TabelaOpcodes.DIVR:
                if (sinal(ins.r1) == 0) {
                    throw new ErroExecucao("DIVR: divisao por zero no endereco "
                            + Palavra.hex(ins.endereco, 6) + ".");
                }
                regs.set(ins.r2, sinal(ins.r2) / sinal(ins.r1));
                return Resultado.CONTINUA;
            case TabelaOpcodes.COMPR:
                regs.comparar(regs.get(ins.r1), regs.get(ins.r2));
                return Resultado.CONTINUA;
            case TabelaOpcodes.RMO:
                regs.set(ins.r2, regs.get(ins.r1));
                return Resultado.CONTINUA;
            case TabelaOpcodes.CLEAR:
                regs.set(ins.r1, 0);
                return Resultado.CONTINUA;
            case TabelaOpcodes.TIXR: {
                int x = Palavra.mascarar(regs.get(Registradores.X) + 1);
                regs.set(Registradores.X, x);
                regs.comparar(x, regs.get(ins.r1));
                return Resultado.CONTINUA;
            }

            default:
                return Resultado.NAO_TRATADA;
        }
    }

    private int sinal(int registrador) {
        return Palavra.paraSinalizado(regs.get(registrador));
    }
}
