package sicxe.montador;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Junta as pecas do montador: analisa o fonte, roda as duas passagens e
// produz o programa objeto e a listagem.

public class Montador {

    public static class Resultado {
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
    // vira um simbolo de substituicao em vez de derrubar o programa.
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