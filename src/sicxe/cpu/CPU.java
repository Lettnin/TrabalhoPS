package sicxe.cpu;

import sicxe.maquina.ErroExecucao;
import sicxe.maquina.Memoria;
import sicxe.maquina.Palavra;
import sicxe.maquina.Registradores;

public class CPU {
    private final Memoria mem;
    private final Registradores regs;
    private final Enderecamento ender;
    private final InstrucoesMemoria instMemoria;
    private final InstrucoesRegistrador instRegistrador;




    public CPU(Memoria mem, Registradores regs){
        this.mem = mem;
        this.regs = regs;
        this.ender = new Enderecamento(mem, regs);
        this.instMemoria = new InstrucoesMemoria(mem, regs, ender);
        this.instRegistrador = new InstrucoesRegistrador(mem, regs, ender);
    }
}