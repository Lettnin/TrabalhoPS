package sicxe.macro;

// Erro encontrado pelo processador de macros: MEND sem MACRO, parametros
// demais na chamada, macro que chama a si mesma...
// Quem lanca nao sabe em que linha esta. O ProcessadorMacros captura,
// junta o numero da linha e segue para a linha seguinte, para mostrar
// todos os erros de uma vez (como o montador faz com o ErroMontagem).

public class ErroMacro extends RuntimeException {
    public ErroMacro(String mensagem) {
        super(mensagem);
    }
}
