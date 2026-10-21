package sicxe.montador;

import sicxe.maquina.Palavra;

public class Linha {

    public int numero;              // numero da linha no arquivo, a partir de 1
    public String texto;            // a linha exatamente como foi escrita
    public boolean vazia;           // linha em branco ou so de comentario

    public String rotulo;           // null se a linha nao tem rotulo
    public String operacao;         // em maiusculas e sem o '+'
    public String operando;         // como foi escrito: "#TABELA", "VETOR,X", "S,X"
    public String expressao;        // o operando sem '#', '@' e ',X'

    public boolean formato4;        // escrita com '+'
    public boolean imediato;        // '#'
    public boolean indireto;        // '@'
    public boolean indexado;        // ',X'

    public int endereco = -1;       // contador de posicao (LC) desta linha
    public int tamanho;             // quantos bytes a linha ocupa na memoria

    public int[] codigo = new int[0];       // bytes gerados pela Passagem 2
    public int enderecoModificacao = -1;    // onde comeca o campo a corrigir na relocacao
    public int meiosBytesModificacao;       // 5 no formato 4, 6 no WORD; 0 = sem registro M

    public String erro;             // null se a linha esta certa

    public String codigoEmHex() {
        StringBuilder sb = new StringBuilder();
        for (int by : codigo) {
            sb.append(Palavra.hex(by, 2));
        }
        return sb.toString();
    }
}
