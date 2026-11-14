package sicxe.macro;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

// Tabela de macros: nome -> definicao. Faz o papel das duas tabelas dos
// slides: a tabela de nomes de macro (para reconhecer uma chamada) e a
// tabela de definicao de macros (o esqueleto de cada uma).
//
// Definir de novo uma macro que ja existe troca a definicao antiga pela
// nova (redefinicao). Com definicao aninhada isso acontece toda vez que a
// macro de fora e expandida: cada expansao define a de dentro outra vez.

public class TabelaMacros {

    // LinkedHashMap: busca rapida pelo nome e guarda a ordem de definicao.
    private final Map<String, Macro> macros = new LinkedHashMap<>();

    public void definir(Macro macro) {
        macros.put(macro.nome, macro);
    }

    public boolean existe(String nome) {
        return nome != null && macros.containsKey(nome.toUpperCase());
    }

    public Macro buscar(String nome) {
        return macros.get(nome.toUpperCase());
    }

    public Collection<Macro> todas() {
        return macros.values();
    }

    public int tamanho() {
        return macros.size();
    }

    // A tabela inteira em texto, com o esqueleto de cada macro, para a
    // janela e para o terminal.
    public String comoTexto() {
        if (macros.isEmpty()) {
            return "Nenhuma macro definida.\n";
        }
        StringBuilder sb = new StringBuilder();
        for (Macro m : macros.values()) {
            sb.append(m.prototipo()).append("      (definida na linha ")
              .append(m.linhaDefinicao).append(")\n");
            for (String linha : m.esqueleto) {
                sb.append("    ").append(linha).append('\n');
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
