package sicxe.cpu;

/**
 * O que aconteceu depois de tentar executar uma instrucao.
 *
 * NAO_TRATADA -> esta classe nao conhece esse opcode, CPU tenta a outra.
 * CONTINUA    -> instrucao executada, a maquina segue.
 * PARAR       -> instrucao executada e o programa terminou.
 */
public enum Resultado {
    NAO_TRATADA,
    CONTINUA,
    PARAR
}
