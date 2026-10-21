package sicxe.montador;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Tabela de simbolos (ST): cada rotulo e o valor dele.
// A Passagem 1 preenche, a Passagem 2 consulta.
// Relativo: endereco do programa. Absoluto: numero independente da carga.
public class TabelaSimbolos {

    public static class Simbolo {
        public final String nome;
        public final int valor;
        public final boolean relativo;
        public final int linha;         // linha do fonte onde foi definido

        public Simbolo(String nome, int valor, boolean relativo, int linha) {
            this.nome = nome;
            this.valor = valor;
            this.relativo = relativo;
            this.linha = linha;
        }
    }

    // Mantem a ordem de definicao para a listagem e a interface.
    private final Map<String, Simbolo> simbolos = new LinkedHashMap<>();

    public void definir(String nome, int valor, boolean relativo, int linha) {
        Simbolo existente = simbolos.get(nome);
        if (existente != null) {
            throw new ErroMontagem("simbolo duplicado: " + nome
                    + " (ja definido na linha " + existente.linha + ")");
        }
        simbolos.put(nome, new Simbolo(nome, valor, relativo, linha));
    }

    public boolean contem(String nome) {
        return simbolos.containsKey(nome);
    }

    public Simbolo buscar(String nome) {
        Simbolo s = simbolos.get(nome);
        if (s == null) {
            throw new ErroMontagem("simbolo indefinido: " + nome);
        }
        return s;
    }

    public List<Simbolo> todos() {
        return new ArrayList<>(simbolos.values());
    }
}
