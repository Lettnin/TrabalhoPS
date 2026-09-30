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
}