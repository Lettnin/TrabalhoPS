package sicxe.macro;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import sicxe.montador.Montador;



public class MacroMontador {

    public static final String ARQUIVO_EXPANDIDO = "MASMAPRG.ASM";

    public static class Resultado {
        public ProcessadorMacros.Resultado macros;
        public Montador.Resultado montagem;     // null se o processador de macros achou erro

        public boolean temErros() {
            return macros.temErros() || montagem.temErros();
        }

        // Os erros das duas etapas, dizendo de qual veio cada um.
        public List<String> erros() {
            List<String> lista = new ArrayList<>();
            for (String erro : macros.erros) {
                lista.add("macros, " + erro);
            }
            if (montagem != null) {
                for (String erro : montagem.erros) {
                    lista.add("montador (" + ARQUIVO_EXPANDIDO + "), " + erro);
                }
            }
            return lista;
        }
    }

    // Com erro nas macros, o montador nem e chamado.
    public Resultado processar(List<String> fonte) {
        Resultado r = new Resultado();
        r.macros = new ProcessadorMacros().processar(fonte);
        if (!r.macros.temErros()) {
            r.montagem = new Montador().montar(r.macros.expandido);
        }
        return r;
    }

    public Resultado processarArquivo(File fonte) throws IOException {
        Resultado r = processar(Montador.lerLinhas(fonte));
        if (r.macros.temErros()) {
            return r;
        }
        Montador.gravar(arquivoExpandido(fonte), String.join("\n", r.macros.expandido) + "\n");
        String semExtensao = Montador.semExtensao(fonte.getPath());
        Montador.gravar(new File(semExtensao + ".lst"), r.montagem.listagem);
        if (!r.montagem.temErros()) {
            Montador.gravar(new File(semExtensao + ".obj"), r.montagem.programaObjeto);
        }
        return r;
    }

    public static File arquivoExpandido(File fonte) {
        return new File(fonte.getAbsoluteFile().getParentFile(), ARQUIVO_EXPANDIDO);
    }
}
