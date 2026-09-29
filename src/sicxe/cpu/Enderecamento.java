package sicxe.cpu;

import sicxe.maquina.ErroExecucao;
import sicxe.maquina.Memoria;
import sicxe.maquina.Palavra;
import sicxe.maquina.Registradores;

/**
 * O coracao conceitual do SIC/XE: descobrir DE ONDE vem o operando.
 *
 * Os 6 bits n i x b p e dizem como montar o "endereco alvo" (TA, target address):
 *
 *   e  -> 0: instrucao de 3 bytes (disp de 12 bits)   1: 4 bytes (endereco de 20 bits)
 *   p  -> 1: o disp e um deslocamento COM SINAL somado ao PC da PROXIMA instrucao
 *   b  -> 1: o disp e um deslocamento SEM SINAL somado ao registrador B
 *   x  -> 1: soma ainda o registrador X (indexado)
 *   n i-> 0 0: instrucao no padrao SIC antigo (endereco direto de 15 bits)
 *         0 1: IMEDIATO  - o valor calculado JA E o operando
 *         1 0: INDIRETO  - o valor calculado e o endereco de onde ler o endereco final
 *         1 1: DIRETO    - o valor calculado e o endereco do operando
 */
public class Enderecamento {

    private final Memoria mem;
    private final Registradores regs;

    public Enderecamento(Memoria mem, Registradores regs) {
        this.mem = mem;
        this.regs = regs;
    }

    public boolean ehImediato(Instrucao ins) {
        return !ins.modoSIC && !ins.n && ins.i;
    }

    public boolean ehIndireto(Instrucao ins) {
        return !ins.modoSIC && ins.n && !ins.i;
    }

    /**
     * Calcula o endereco alvo (TA). No modo imediato, o "TA" e o proprio valor.
     */
    public int calcularAlvo(Instrucao ins) {
        int alvo;

        if (ins.modoSIC) {
            alvo = ins.disp;                            // 15 bits, direto
        } else if (ins.e) {
            alvo = ins.disp;                            // 20 bits, direto
        } else if (ins.p) {
            // ATENCAO: o PC usado aqui e o da PROXIMA instrucao.
            alvo = ins.endereco + ins.tamanho + Palavra.sinal12(ins.disp);
        } else if (ins.b) {
            alvo = regs.get(Registradores.B) + ins.disp;  // disp sem sinal
        } else {
            alvo = ins.disp;                            // direto, 12 bits
        }

        if (ins.x) {
            alvo += regs.get(Registradores.X);
        }

        alvo = Palavra.mascarar(alvo);

        if (ehIndireto(ins)) {
            alvo = mem.lerPalavra(alvo);
        }
        return alvo;
    }

    /** Valor de 24 bits do operando (usado por LDA, ADD, COMP, ...). */
    public int valorPalavra(Instrucao ins) {
        int alvo = calcularAlvo(ins);
        if (ehImediato(ins)) {
            return Palavra.mascarar(alvo);
        }
        return mem.lerPalavra(alvo);
    }

    /** Valor de 1 byte do operando (usado por LDCH). */
    public int valorByte(Instrucao ins) {
        int alvo = calcularAlvo(ins);
        if (ehImediato(ins)) {
            return alvo & 0xFF;
        }
        return mem.lerByte(alvo);
    }

    /** Endereco de destino (usado por STA, STCH, J, JSUB, ...). */
    public int enderecoDestino(Instrucao ins) {
        if (ehImediato(ins)) {
            throw new ErroExecucao(ins.mnemonico + " nao aceita modo imediato (endereco "
                    + Palavra.hex(ins.endereco, 6) + ").");
        }
        return calcularAlvo(ins);
    }
}
