package sicxe.cpu;

import sicxe.maquina.ErroExecucao;
import sicxe.maquina.Palavra;
import sicxe.maquina.Registradores;

/**
 * Instrucoes de formato 2 (que so mexem em registradores) e a familia
 * de desvios (J, JEQ, JGT, JLT, JSUB, RSUB).
 *
 * Convencao de parada adotada pelo grupo: o SIC/XE nao tem instrucao HALT.
 * O programa termina quando executa RSUB com o registrador L igual a zero
 * (ou seja, "retorna" sem ter sido chamado) ou quando um J salta para o
 * proprio endereco (laco infinito proposital).
 */
public class InstrucoesRegistrador {

    private final Registradores regs;
    private final Enderecamento ender;

    public InstrucoesRegistrador(Registradores regs, Enderecamento ender) {
        this.regs = regs;
        this.ender = ender;
    }

    public Resultado executar(Instrucao ins) {
        switch (ins.opcode) {

            // ---------- Formato 2 ----------
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
            case TabelaOpcodes.SHIFTL: {
                int n = ins.r2 + 1;                       // o campo guarda n-1
                int v = regs.get(ins.r1);
                int desl = ((v << n) | (v >>> (24 - n)));  // circular
                regs.set(ins.r1, desl);
                return Resultado.CONTINUA;
            }
            case TabelaOpcodes.SHIFTR: {
                int n = ins.r2 + 1;                       // o campo guarda n-1
                int v = Palavra.paraSinalizado(regs.get(ins.r1));
                regs.set(ins.r1, v >> n);                  // aritmetico, mantem o sinal
                return Resultado.CONTINUA;
            }
            case TabelaOpcodes.TIXR: {
                int x = Palavra.mascarar(regs.get(Registradores.X) + 1);
                regs.set(Registradores.X, x);
                regs.comparar(x, regs.get(ins.r1));
                return Resultado.CONTINUA;
            }

            // ---------- Desvios ----------
            case TabelaOpcodes.J: {
                int destino = ender.enderecoDestino(ins);
                if (destino == ins.endereco) {
                    regs.setPC(destino);
                    return Resultado.PARAR;               // J para si mesmo = fim
                }
                regs.setPC(destino);
                return Resultado.CONTINUA;
            }
            case TabelaOpcodes.JEQ:
                return desvioCondicional(ins, Registradores.CC_IGUAL);
            case TabelaOpcodes.JLT:
                return desvioCondicional(ins, Registradores.CC_MENOR);
            case TabelaOpcodes.JGT:
                return desvioCondicional(ins, Registradores.CC_MAIOR);
            case TabelaOpcodes.JSUB: {
                int destino = ender.enderecoDestino(ins);
                regs.set(Registradores.L, regs.getPC());   // PC ja aponta p/ a proxima
                regs.setPC(destino);
                return Resultado.CONTINUA;
            }
            case TabelaOpcodes.RSUB: {
                int retorno = regs.get(Registradores.L);
                if (retorno == 0) {
                    return Resultado.PARAR;                // fim do programa
                }
                regs.setPC(retorno);
                return Resultado.CONTINUA;
            }

            default:
                return Resultado.NAO_TRATADA;
        }
    }

    private Resultado desvioCondicional(Instrucao ins, int ccEsperado) {
        int destino = ender.enderecoDestino(ins);
        if (regs.getCC() == ccEsperado) {
            regs.setPC(destino);
        }
        return Resultado.CONTINUA;
    }

    private int sinal(int registrador) {
        return Palavra.paraSinalizado(regs.get(registrador));
    }
}
