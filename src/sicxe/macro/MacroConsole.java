package sicxe.macro;

import java.io.File;
import java.io.IOException;

import sicxe.montador.Montador;


public class MacroConsole {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java -cp bin sicxe.macro.MacroConsole <arquivo.asm>");
            return;
        }
        File fonte = new File(args[0]);
        MacroMontador.Resultado r;
        try {
            r = new MacroMontador().processarArquivo(fonte);
        } catch (IOException e) {
            System.out.println("Nao foi possivel ler o arquivo: " + e.getMessage());
            System.exit(1);
            return;
        }

        System.out.println("Tabela de macros:");
        System.out.println(r.macros.macros.comoTexto());
        if (r.macros.temErros()) {
            mostrarErros(r);
            System.out.println("O " + MacroMontador.ARQUIVO_EXPANDIDO + " NAO foi gerado.");
            System.exit(1);
        }

        System.out.println(r.macros.expansoes + " chamada(s) expandida(s). Fonte expandido em "
                + MacroMontador.arquivoExpandido(fonte).getPath() + "\n");
        System.out.println(r.montagem.listagem);
        if (r.montagem.temErros()) {
            mostrarErros(r);
            System.out.println("O programa objeto NAO foi gerado.");
            System.exit(1);
        }
        System.out.println("Programa objeto (" + Montador.semExtensao(fonte.getPath()) + ".obj):");
        System.out.print(r.montagem.programaObjeto);
    }

    private static void mostrarErros(MacroMontador.Resultado r) {
        System.out.println(r.erros().size() + " erro(s):");
        for (String erro : r.erros()) {
            System.out.println("  " + erro);
        }
    }
}
