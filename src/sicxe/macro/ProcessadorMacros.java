package sicxe.macro;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ProcessadorMacros {

    public static class Resultado {
        public List<String> expandido = new ArrayList<>();
        public TabelaMacros macros = new TabelaMacros();
        public List<String> erros = new ArrayList<>();
        public int expansoes;

        public boolean temErros() {
            return !erros.isEmpty();
        }
    }

    private static class Expansao {
        final Macro macro;
        final String[] valores;
        int proxima;

        Expansao(Macro macro, String[] valores) {
            this.macro = macro;
            this.valores = valores;
        }
    }

    private List<String> fonte;
    private int indiceFonte;
    private int linhaAtual;
    private final Deque<Expansao> expansoes = new ArrayDeque<>();
    private Resultado r;

    public Resultado processar(List<String> fonte) {
        this.fonte = fonte;
        indiceFonte = 0;
        linhaAtual = 0;
        expansoes.clear();
        r = new Resultado();

        String texto;
        while ((texto = proximaLinha()) != null) {
            String onde = onde();
            LinhaMacro linha = LinhaMacro.analisar(texto);
            try {
                if (linha.iniciaDefinicao()) {
                    definir(linha);
                } else if (linha.terminaDefinicao()) {
                    throw new ErroMacro(linha.operacao + " sem MACRO correspondente");
                } else if (r.macros.existe(linha.operacao)) {
                    expandir(linha);
                } else {
                    r.expandido.add(texto);
                }
            } catch (ErroMacro e) {
                r.erros.add(onde + e.getMessage());
            }
        }
        return r;
    }

    private String proximaLinha() {
        while (!expansoes.isEmpty()) {
            Expansao topo = expansoes.peek();
            if (topo.proxima < topo.macro.esqueleto.size()) {
                String linha = topo.macro.esqueleto.get(topo.proxima++);
                return Substituicao.expandir(linha, topo.valores);
            }
            expansoes.pop();
        }
        if (indiceFonte < fonte.size()) {
            linhaAtual = indiceFonte + 1;
            return fonte.get(indiceFonte++);
        }
        return null;
    }

    private void definir(LinhaMacro prototipo) {
        String nome = prototipo.rotulo;
        Macro macro = null;
        ErroMacro erroNoPrototipo = null;
        try {
            macro = lerPrototipo(prototipo);
        } catch (ErroMacro e) {
            erroNoPrototipo = e;
        }

        List<List<String>> formais = new ArrayList<>();
        formais.add(macro != null ? macro.formaisComSerie() : List.of(".SER"));

        while (true) {
            String texto = proximaLinha();
            if (texto == null) {
                throw new ErroMacro("falta o MEND da macro " + nome);
            }
            LinhaMacro linha = LinhaMacro.analisar(texto);
            if (linha.vazia) {
                continue;
            }
            if (linha.iniciaDefinicao()) {
                formais.add(formaisDaMacroDeDentro(linha));
            } else if (linha.terminaDefinicao()) {
                formais.remove(formais.size() - 1);
                if (formais.isEmpty()) {
                    break;
                }
            }
            if (macro != null) {
                macro.esqueleto.add(Substituicao.preparar(texto, formais));
            }
        }

        if (erroNoPrototipo != null) {
            throw erroNoPrototipo;
        }
        r.macros.definir(macro);
    }

    private Macro lerPrototipo(LinhaMacro prototipo) {
        String nome = prototipo.rotulo;
        if (nome == null) {
            throw new ErroMacro("falta o nome da macro, que vai no rotulo: NOME MACRO &A,&B");
        }
        if (!Character.isLetter(nome.charAt(0)) || !nome.chars().allMatch(Character::isLetterOrDigit)) {
            throw new ErroMacro("nome de macro invalido: " + nome + " (use so letras e digitos)");
        }
        Macro macro = new Macro(nome, linhaAtual);
        Argumentos.lerFormais(macro, prototipo.operando);
        return macro;
    }

    private List<String> formaisDaMacroDeDentro(LinhaMacro linha) {
        try {
            Macro deDentro = new Macro(String.valueOf(linha.rotulo), linhaAtual);
            Argumentos.lerFormais(deDentro, linha.operando);
            return deDentro.formaisComSerie();
        } catch (ErroMacro e) {
            return List.of(".SER");
        }
    }

    private void expandir(LinhaMacro chamada) {
        Macro macro = r.macros.buscar(chamada.operacao);
        for (Expansao e : expansoes) {
            if (e.macro.nome.equals(macro.nome)) {
                throw new ErroMacro("a macro " + macro.nome
                        + " chama a si mesma, e a expansao nunca terminaria");
            }
        }
        String[] valores = Argumentos.associar(macro, chamada.operando);
        r.expansoes++;
        valores[0] = String.format("%04d", r.expansoes);

        r.expandido.add("." + chamada.texto);
        if (chamada.rotulo != null) {
            r.expandido.add(String.format("%-8s EQU     *", chamada.rotulo));
        }
        expansoes.push(new Expansao(macro, valores));
    }

    private String onde() {
        if (expansoes.isEmpty()) {
            return "linha " + linhaAtual + ": ";
        }
        return "linha " + linhaAtual + " (na expansao de " + expansoes.peek().macro.nome + "): ";
    }
}
