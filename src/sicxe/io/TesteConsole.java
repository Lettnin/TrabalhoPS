package sicxe.io;

import java.io.File;

import sicxe.cpu.CPU;
import sicxe.maquina.Memoria;
import sicxe.maquina.Palavra;
import sicxe.maquina.Registradores;

public class TesteConsole {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java -cp bin sicxe.io.TesteConsole <arquivo> [end1 end2 ...]");
            return;
        }

        Memoria mem = new Memoria();
        Registradores regs = new Registradores();
        CPU cpu = new CPU(mem, regs);

        Carregador.ResultadoCarga r = new Carregador().carregar(new File(args[0]), mem);
        cpu.prepararExecucao(r.enderecoExecucao);
        System.out.println("Carregado: " + r.bytesCarregados + " bytes, PC inicial = "
                + Palavra.hex(r.enderecoExecucao, 6));

        while (!cpu.isParada()) {
            int pc = regs.getPC();
            cpu.passo();
            System.out.println("  " + Palavra.hex(pc, 6) + "  "
                    + cpu.getTextoUltimaInstrucao()
                    + "   -> A=" + Palavra.hex(regs.get(Registradores.A), 6)
                    + " X=" + Palavra.hex(regs.get(Registradores.X), 6)
                    + " L=" + Palavra.hex(regs.get(Registradores.L), 6)
                    + " B=" + Palavra.hex(regs.get(Registradores.B), 6)
                    + " CC=" + regs.getCCTexto());
        }

        System.out.println(cpu.getMensagem());
        for (int k = 1; k < args.length; k++) {
            int endereco = Integer.parseInt(args[k], 16);
            System.out.println("  memoria[" + Palavra.hex(endereco, 6) + "] = "
                    + Palavra.hex(mem.lerPalavra(endereco), 6)
                    + "  (" + Palavra.paraSinalizado(mem.lerPalavra(endereco)) + ")");
        }
    }
}
