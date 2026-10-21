package sicxe.montador;

// Erro encontrado durante a montagem: simbolo indefinido, operacao
// desconhecida, constante mal escrita, deslocamento que nao cabe
// Quem lanca nao sabe em que linha esta. Quem captura (as passagens)
// guarda a mensagem na propria Linha e segue para a linha seguinte,
// para o montador mostrar todos os erros de uma vez.

public class ErroMontagem extends RuntimeException {
    public ErroMontagem(String mensagem) {
        super(mensagem);
    }
}
