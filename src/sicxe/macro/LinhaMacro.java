package sicxe.macro;

import java.util.List;

import sicxe.montador.AnalisadorLinha;

public class LinhaMacro {

    public String texto;        
    public boolean vazia;       
    public String rotulo;       
    public String operacao;     
    public String operando;     
    public static LinhaMacro analisar(String texto) {
        LinhaMacro linha = new LinhaMacro();
        linha.texto = texto;
        String t = texto.stripTrailing();
        if (t.isBlank() || t.stripLeading().startsWith(".")) {
            linha.vazia = true;
            return linha;
        }
        List<String> palavras = AnalisadorLinha.palavras(t);
        int k = 0;
        if (t.charAt(0) != ' ' && t.charAt(0) != '\t') {
            linha.rotulo = palavras.get(k++).toUpperCase();
        }
        if (k < palavras.size() && !palavras.get(k).startsWith(".")) {
            linha.operacao = palavras.get(k++).toUpperCase();
        }
        if (k < palavras.size() && !palavras.get(k).startsWith(".")) {
            linha.operando = palavras.get(k);
        }
        return linha;
    }

    public boolean iniciaDefinicao() {
        return "MACRO".equals(operacao) || "MCDEFN".equals(operacao);
    }

    public boolean terminaDefinicao() {
        return "MEND".equals(operacao) || "MCEND".equals(operacao);
    }
}