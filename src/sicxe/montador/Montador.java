package sicxe.montador;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Junta as pecas do montador: analisa o fonte, roda as duas passagens e produz o programa objeto e a listagem.

public class Montador {

    public static class Resultado {
        public List<Linha> linhas = new ArrayList<>();
        public TabelaSimbolos simbolos = new TabelaSimbolos();
        public List<String> erros = new ArrayList<>();
        public String nomePrograma = "";
        public int enderecoInicial;
        public int tamanhoPrograma;
        public int enderecoExecucao;
        public String programaObjeto = "";      // fica vazio se houve erro
        public String listagem = "";

        public boolean temErros() {
            return !erros.isEmpty();
        }
    }

    public Resultado montar(List<String> fonte) {
        Resultado r = new Resultado();
        for (int k = 0; k < fonte.size(); k++) {
            r.linhas.add(AnalisadorLinha.analisar(k + 1, fonte.get(k)));
        }

        Passagem1 p1 = new Passagem1(r.simbolos);
        p1.executar(r.linhas);
        Passagem2 p2 = new Passagem2(r.simbolos);
        p2.executar(r.linhas, p1.getEnderecoInicial());

        r.nomePrograma = p1.getNomePrograma();
        r.enderecoInicial = p1.getEnderecoInicial();
        r.tamanhoPrograma = p1.getTamanhoPrograma();
        r.enderecoExecucao = p2.getEnderecoExecucao();

        for (Linha linha : r.linhas) {
            if (linha.erro != null) {
                r.erros.add("linha " + linha.numero + ": " + linha.erro);
            }
        }
        if (p1.getLinhaEnd() == null) {
            r.erros.add("falta a diretiva END no fim do programa");
        }

        r.listagem = Listagem.gerar(r.linhas, r.simbolos);
        if (!r.temErros()) {
            r.programaObjeto = ProgramaObjeto.gerar(r.nomePrograma, r.enderecoInicial,
                    r.tamanhoPrograma, r.enderecoExecucao, r.linhas);
        }
        return r;
    }

    // Monta um arquivo .asm e grava ao lado dele o .lst (sempre) e o .obj (so se nao houve erro).
    public Resultado montarArquivo(File fonte) throws IOException {
        Resultado r = montar(lerLinhas(fonte));
        String semExtensao = semExtensao(fonte.getPath());
        gravar(new File(semExtensao + ".lst"), r.listagem);
        if (!r.temErros()) {
            gravar(new File(semExtensao + ".obj"), r.programaObjeto);
        }
        return r;
    }

    public static List<String> lerLinhas(File arquivo) throws IOException {
        String texto = new String(Files.readAllBytes(arquivo.toPath()), StandardCharsets.UTF_8);
        return Arrays.asList(texto.split("\\r?\\n"));
    }

    public static void gravar(File arquivo, String conteudo) throws IOException {
        Files.write(arquivo.toPath(), conteudo.getBytes(StandardCharsets.UTF_8));
    }

    public static String semExtensao(String caminho) {
        int ponto = caminho.lastIndexOf('.');
        int barra = Math.max(caminho.lastIndexOf('/'), caminho.lastIndexOf('\\'));
        return ponto > barra ? caminho.substring(0, ponto) : caminho;
    }
}
