package sicxe.cpu;

import sicxe.maquina.ErroExecucao;
import sicxe.maquina.Memoria;
import sicxe.maquina.Palavra;
import sicxe.maquina.Registradores;

public class CPU {

    private final Memoria mem;
    private final Registradores regs;
    private final Decodificador decodificador;
    private final Enderecamento ender;
    private final InstrucoesMemoria instrMemoria;
    private final InstrucoesRegistrador instrRegistrador;

    private boolean parada = true;
    private boolean erro = false;
    private String mensagem = "Nenhum programa carregado.";
    private Instrucao ultimaInstrucao;
    private String textoUltimaInstrucao = "";
    private int enderecoInicial = 0;
    private long contadorInstrucoes = 0;

    public CPU(Memoria mem, Registradores regs) {
        this.mem = mem;
        this.regs = regs;
        this.decodificador = new Decodificador();
        this.ender = new Enderecamento(mem, regs);
        this.instrMemoria = new InstrucoesMemoria(mem, regs, ender);
        this.instrRegistrador = new InstrucoesRegistrador(regs, ender);
    }

    /** Executa exatamente UMA instrucao. */
    public void passo() {
        if (parada) {
            return;
        }
        mem.limparEscritasRecentes();
        try {
            Instrucao ins = decodificador.decodificar(mem, regs.getPC());
            ultimaInstrucao = ins;
            textoUltimaInstrucao = decodificador.paraTexto(ins);

            regs.setPC(ins.endereco + ins.tamanho);

            Resultado r = instrMemoria.executar(ins);
            if (r == Resultado.NAO_TRATADA) {
                r = instrRegistrador.executar(ins);
            }
            if (r == Resultado.NAO_TRATADA) {
                throw new ErroExecucao("Instrucao reconhecida mas nao implementada: "
                        + ins.mnemonico);
            }

            contadorInstrucoes++;

            if (r == Resultado.PARAR) {
                parada = true;
                mensagem = "Execucao encerrada por " + ins.mnemonico
                        + " apos " + contadorInstrucoes + " instrucoes.";
            } else {
                mensagem = "Executada: " + textoUltimaInstrucao;
            }
        } catch (ErroExecucao e) {
            parada = true;
            erro = true;
            mensagem = "ERRO: " + e.getMessage();
        }
    }

    public void executarTudo(int limiteDeInstrucoes) {
        int executadas = 0;
        while (!parada && executadas < limiteDeInstrucoes) {
            passo();
            executadas++;
        }
        if (!parada) {
            parada = true;
            erro = true;
            mensagem = "ERRO: limite de " + limiteDeInstrucoes
                    + " instrucoes atingido (provavel laco infinito).";
        }
    }

    public void prepararExecucao(int enderecoInicial) {
        this.enderecoInicial = enderecoInicial;
        regs.limpar();
        regs.setPC(enderecoInicial);
        parada = false;
        erro = false;
        contadorInstrucoes = 0;
        ultimaInstrucao = null;
        textoUltimaInstrucao = "";
        mensagem = "Pronto. PC = " + Palavra.hex(enderecoInicial, 6);
    }

    public void reset() {
        prepararExecucao(enderecoInicial);
    }

    public boolean isParada(){
        return parada;
    }
    public boolean isErro(){
        return erro; 
    }
    public String getMensagem(){
        return mensagem;
    }
    public Instrucao getUltimaInstrucao(){
        return ultimaInstrucao;
    }
    public String getTextoUltimaInstrucao(){
        return textoUltimaInstrucao;
    }
    public long getContadorInstrucoes(){
        return contadorInstrucoes;
    }
    public int getEnderecoInicial(){
        return enderecoInicial; 
    }
    public Decodificador getDecodificador(){
        return decodificador;
    }
}
