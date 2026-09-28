package sicxe.maquina;

 // Erro detectado durante a execucao da maquina virtual.
 // Exemplos: endereco fora da memoria, opcode desconhecido, divisao por zero.
 // Usamos RuntimeException para nao obrigar "throws" em toda a cadeia de chamadas.
 
public class ErroExecucao extends RuntimeException {
    public ErroExecucao(String mensagem) {
        super(mensagem);
    }
}
