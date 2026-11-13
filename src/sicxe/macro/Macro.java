package sicxe.macro;

import java.util.ArrayList;
import java.util.List;

// Uma macro como fica guardada na tabela de definicao de macros:
//
//   prototipo:  o nome e os parametros formais (com valor padrao, se tiver);
//   esqueleto:  as linhas do corpo, com cada parametro formal ja trocado
//               pela posicao dele no prototipo.
//
//   Fonte                              Guardado
//   SOMA     MACRO   &A,&B,&DEST       nome = SOMA, formais = [&A, &B, &DEST]
//            LDA     &A                         LDA     ?1
//            ADD     &B                         ADD     ?2
//            STA     &DEST                      STA     ?3
//            MEND
//
// Os slides escrevem #1, #2. Aqui e ?1, ?2, como no livro do Beck, porque no
// SIC/XE o # ja quer dizer imediato: "LDA #&A" fica "LDA #?1".
// O ?0 e o numero de serie da expansao (o .SER dos slides).

public class Macro {

    public final String nome;
    public final List<String> formais = new ArrayList<>();    // em maiusculas, como escritos: "&A"
    public final List<String> padroes = new ArrayList<>();    // valor padrao de cada formal ("" = nenhum)
    public final List<String> esqueleto = new ArrayList<>();  // o corpo, com ?1, ?2... no lugar dos formais
    public final int linhaDefinicao;

    public Macro(String nome, int linhaDefinicao) {
        this.nome = nome;
        this.linhaDefinicao = linhaDefinicao;
    }

    public void adicionarFormal(String nome, String padrao) {
        if (indiceFormal(nome) > 0) {
            throw new ErroMacro("parametro formal repetido: " + nome);
        }
        formais.add(nome);
        padroes.add(padrao);
    }

    // Posicao do formal no prototipo (a partir de 1), ou -1 se nao existe.
    // Aceita o nome sem o '&': na chamada por palavra-chave se escreve
    // PASSO=5 para o formal &PASSO.
    public int indiceFormal(String nome) {
        String n = nome.toUpperCase();
        for (int k = 0; k < formais.size(); k++) {
            String f = formais.get(k);
            if (f.equals(n) || f.equals("&" + n)) {
                return k + 1;
            }
        }
        return -1;
    }

    // Os formais na numeracao do esqueleto: na posicao 0 fica o .SER, que e
    // o "parametro formal definido pelo sistema" dos slides.
    public List<String> formaisComSerie() {
        List<String> lista = new ArrayList<>();
        lista.add(".SER");
        lista.addAll(formais);
        return lista;
    }

    // "INCR &VAR,&PASSO=1", para mostrar na tabela de macros.
    public String prototipo() {
        StringBuilder sb = new StringBuilder(nome);
        for (int k = 0; k < formais.size(); k++) {
            sb.append(k == 0 ? " " : ",").append(formais.get(k));
            if (!padroes.get(k).isEmpty()) {
                sb.append('=').append(padroes.get(k));
            }
        }
        return sb.toString();
    }
}
