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
}