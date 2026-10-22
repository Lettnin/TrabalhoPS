package sicxe.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

import sicxe.maquina.Palavra;
import sicxe.montador.Montador;
import sicxe.montador.TabelaSimbolos;

// Janela do montador (Etapa 2). A esquerda fica o fonte, que da para
// editar. A direita, em abas, a listagem, a tabela de simbolos e o
// programa objeto. Embaixo, as mensagens: os erros com o numero da linha,
// ou o resumo da montagem.

public class JanelaMontador extends JFrame {

    private static final Color COR_ERRO = new Color(215, 40, 40);
    private static final Color COR_OK = new Color(30, 130, 60);

    private final JanelaPrincipal simulador;
    private final Montador montador = new Montador();

    private final JTextArea editor = new JTextArea();
    private final JTextArea numerosDasLinhas = new JTextArea("  1 ");
    private final JTextArea listagem = new JTextArea();
    private final JTextArea programaObjeto = new JTextArea();
    private final JTextArea mensagens = new JTextArea(4, 40);
    private final DefaultTableModel modeloSimbolos =
            new DefaultTableModel(new String[] { "Simbolo", "Valor", "Tipo", "Linha" }, 0);
    private final JTabbedPane abas = new JTabbedPane();

    private File arquivoFonte;              // null enquanto nada foi aberto ou salvo
    private Montador.Resultado resultado;   // a ultima montagem

    public JanelaMontador(JanelaPrincipal simulador) {
        super("Montador SIC/XE");
        this.simulador = simulador;
        montarInterface();
    }

    private void montarInterface() {
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);   // fechar so esconde; o simulador continua aberto
        setSize(1200, 700);
        setLocationRelativeTo(null);

        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 13);
        for (JTextArea area : new JTextArea[] { editor, numerosDasLinhas, listagem, programaObjeto, mensagens }) {
            area.setFont(mono);
        }
        numerosDasLinhas.setEditable(false);
        numerosDasLinhas.setBackground(new Color(235, 235, 235));
        listagem.setEditable(false);
        programaObjeto.setEditable(false);
        mensagens.setEditable(false);

        // Qualquer mudanca no fonte refaz os numeros das linhas.
        editor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { fonteMudou(); }
            public void removeUpdate(DocumentEvent e) { fonteMudou(); }
            public void changedUpdate(DocumentEvent e) { fonteMudou(); }
        });

        JButton botaoAbrir = new JButton("Abrir .asm");
        JButton botaoSalvar = new JButton("Salvar");
        JButton botaoMontar = new JButton("Montar");
        botaoAbrir.addActionListener(e -> abrir());
        botaoSalvar.addActionListener(e -> salvar());
        botaoMontar.addActionListener(e -> montar());

        JPanel barra = new JPanel();
        barra.add(botaoAbrir);
        barra.add(botaoSalvar);
        barra.add(botaoMontar);

        JScrollPane rolagemEditor = new JScrollPane(editor);
        rolagemEditor.setRowHeaderView(numerosDasLinhas);
        rolagemEditor.setBorder(BorderFactory.createTitledBorder("Fonte"));

        JTable tabelaSimbolos = new JTable(modeloSimbolos);
        tabelaSimbolos.setFont(mono);
        tabelaSimbolos.setDefaultEditor(Object.class, null);   // so leitura

        abas.addTab("Listagem", new JScrollPane(listagem));
        abas.addTab("Tabela de simbolos", new JScrollPane(tabelaSimbolos));
        abas.addTab("Programa objeto", new JScrollPane(programaObjeto));

        JSplitPane divisor = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, rolagemEditor, abas);
        divisor.setResizeWeight(0.4);

        JScrollPane rolagemMensagens = new JScrollPane(mensagens);
        rolagemMensagens.setBorder(BorderFactory.createTitledBorder("Mensagens"));

        setLayout(new BorderLayout());
        add(barra, BorderLayout.NORTH);
        add(divisor, BorderLayout.CENTER);
        add(rolagemMensagens, BorderLayout.SOUTH);
    }

    private void abrir() {
        File pasta = arquivoFonte != null ? arquivoFonte.getParentFile() : new File("testes/montador");
        JFileChooser selecionador = new JFileChooser(pasta);
        if (selecionador.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            File arquivo = selecionador.getSelectedFile();
            editor.setText(String.join("\n", Montador.lerLinhas(arquivo)));
            editor.setCaretPosition(0);
            arquivoFonte = arquivo;
            setTitle("Montador SIC/XE - " + arquivo.getName());
            mostrarMensagem("Arquivo aberto: " + arquivo.getName() + ". Clique em Montar.", Color.BLACK);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro ao abrir", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void salvar() {
        if (arquivoFonte == null) {
            JFileChooser selecionador = new JFileChooser(new File("testes/montador"));
            if (selecionador.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            arquivoFonte = selecionador.getSelectedFile();
            setTitle("Montador SIC/XE - " + arquivoFonte.getName());
        }
        try {
            Montador.gravar(arquivoFonte, editor.getText());
            mostrarMensagem("Salvo em " + arquivoFonte.getName() + ".", Color.BLACK);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro ao salvar", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Monta o texto que esta no editor (mesmo que ainda nao tenha sido salvo).
    private void montar() {
        List<String> fonte = Arrays.asList(editor.getText().split("\\r?\\n"));
        resultado = montador.montar(fonte);

        listagem.setText(resultado.listagem);
        listagem.setCaretPosition(0);
        programaObjeto.setText(resultado.programaObjeto);
        modeloSimbolos.setRowCount(0);
        for (TabelaSimbolos.Simbolo s : resultado.simbolos.todos()) {
            modeloSimbolos.addRow(new Object[] {
                s.nome, Palavra.hex(s.valor, 6), s.relativo ? "relativo" : "absoluto", s.linha });
        }

        if (resultado.temErros()) {
            mostrarMensagem(resultado.erros.size() + " erro(s). O programa objeto nao foi gerado.\n"
                    + String.join("\n", resultado.erros), COR_ERRO);
            abas.setSelectedIndex(0);
        } else {
            mostrarMensagem("Montado sem erros: programa " + resultado.nomePrograma
                    + ", comeca em " + Palavra.hex(resultado.enderecoInicial, 6)
                    + ", tamanho " + Palavra.hex(resultado.tamanhoPrograma, 6)
                    + " (" + resultado.tamanhoPrograma + " bytes), execucao em "
                    + Palavra.hex(resultado.enderecoExecucao, 6) + ".", COR_OK);
        }
    }

    private void fonteMudou() {
        StringBuilder sb = new StringBuilder();
        for (int k = 1; k <= editor.getLineCount(); k++) {
            sb.append(String.format("%3d ", k)).append('\n');
        }
        numerosDasLinhas.setText(sb.toString());
    }

    private void mostrarMensagem(String texto, Color cor) {
        mensagens.setForeground(cor);
        mensagens.setText(texto);
        mensagens.setCaretPosition(0);
    }
}
