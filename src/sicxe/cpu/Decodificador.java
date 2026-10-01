package sicxe.cpu;

import sicxe.maquina.ErroExecucao;
import sicxe.maquina.Memoria;
import sicxe.maquina.Palavra;
import sicxe.maquina.Registradores;

public class Decodificador {

    public Instrucao decodificar(Memoria mem, int pc) {
        Instrucao ins = new Instrucao();
        ins.endereco = pc;

        int b0 = mem.lerByte(pc);

        String nome2 = TabelaOpcodes.FORMATO_2.get(b0);
        if (nome2 != null) {
            int b1 = mem.lerByte(pc + 1);
            ins.formato = 2;
            ins.tamanho = 2;
            ins.opcode = b0;
            ins.mnemonico = nome2;
            ins.r1 = (b1 >> 4) & 0x0F;   
            ins.r2 = b1 & 0x0F;         
            ins.bytesBrutos = new int[] { b0, b1 };
            return ins;
        }

        int opcode = b0 & 0xFC;          
        String nome = TabelaOpcodes.FORMATO_34.get(opcode);
        if (nome == null) {
            throw new ErroExecucao("Opcode desconhecido: " + Palavra.hex(b0, 2)
                    + " no endereco " + Palavra.hex(pc, 6));
        }

        ins.opcode = opcode;
        ins.mnemonico = nome;
        ins.n = (b0 & 0x02) != 0;
        ins.i = (b0 & 0x01) != 0;
        ins.modoSIC = !ins.n && !ins.i;

        int b1 = mem.lerByte(pc + 1);

        if (ins.modoSIC) {

            ins.formato = 3;
            ins.tamanho = 3;
            ins.x = (b1 & 0x80) != 0;
            ins.b = false;
            ins.p = false;
            ins.e = false;
            ins.disp = ((b1 & 0x7F) << 8) | mem.lerByte(pc + 2);
            ins.bytesBrutos = new int[] { b0, b1, mem.lerByte(pc + 2) };
            return ins;
        }

        ins.x = (b1 & 0x80) != 0;
        ins.b = (b1 & 0x40) != 0;
        ins.p = (b1 & 0x20) != 0;
        ins.e = (b1 & 0x10) != 0;

        if (ins.e) {
            ins.formato = 4;
            ins.tamanho = 4;
            ins.disp = ((b1 & 0x0F) << 16) | (mem.lerByte(pc + 2) << 8) | mem.lerByte(pc + 3);
            ins.bytesBrutos = new int[] { b0, b1, mem.lerByte(pc + 2), mem.lerByte(pc + 3) };
        } else {
            ins.formato = 3;
            ins.tamanho = 3;
            ins.disp = ((b1 & 0x0F) << 8) | mem.lerByte(pc + 2);
            ins.bytesBrutos = new int[] { b0, b1, mem.lerByte(pc + 2) };
        }
        return ins;
    }

    public String paraTexto(Instrucao ins) {
        if (ins.formato == 2) {
            switch (ins.opcode) {
                case TabelaOpcodes.CLEAR:
                case TabelaOpcodes.TIXR:
                    return preencher(ins.mnemonico, 7) + Registradores.nome(ins.r1);
                case TabelaOpcodes.SHIFTL:
                case TabelaOpcodes.SHIFTR:
                    return preencher(ins.mnemonico, 7) + Registradores.nome(ins.r1)
                            + "," + (ins.r2 + 1);
                default:
                    return preencher(ins.mnemonico, 7) + Registradores.nome(ins.r1)
                            + "," + Registradores.nome(ins.r2);
            }
        }

        if (ins.opcode == TabelaOpcodes.RSUB) {
            return "RSUB";
        }

        int alvo;
        String sufixo;
        if (ins.modoSIC) {
            alvo = ins.disp;
            sufixo = "[SIC]";
        } else if (ins.e) {
            alvo = ins.disp;
            sufixo = "[fmt 4]";
        } else if (ins.p) {
            alvo = ins.endereco + ins.tamanho + Palavra.sinal12(ins.disp);
            sufixo = "[PC" + (Palavra.sinal12(ins.disp) >= 0 ? "+" : "")
                    + Palavra.sinal12(ins.disp) + "]";
        } else if (ins.b) {
            alvo = -1;
            sufixo = "[B+" + Palavra.hex(ins.disp, 3) + "]";
        } else {
            alvo = ins.disp;
            sufixo = "";
        }

        String operando;
        boolean imediato = !ins.modoSIC && !ins.n && ins.i;
        boolean indireto = !ins.modoSIC && ins.n && !ins.i;

        if (alvo < 0) {
            operando = (imediato ? "#" : indireto ? "@" : "") + "(B)";
        } else if (imediato) {
            operando = "#" + alvo;
        } else {
            operando = (indireto ? "@" : "") + Palavra.hex(alvo, 6);
        }
        if (ins.x) {
            operando = operando + ",X";
        }

        String nome = (ins.formato == 4 ? "+" : "") + ins.mnemonico;
        return preencher(nome, 7) + preencher(operando, 11) + sufixo;
    }

    private static String preencher(String texto, int tamanho) {
        StringBuilder sb = new StringBuilder(texto);
        while (sb.length() < tamanho) {
            sb.append(' ');
        }
        return sb.toString();
    }
}