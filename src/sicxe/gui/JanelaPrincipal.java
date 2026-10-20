package sicxe.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.Timer;

import sicxe.cpu.CPU;
import sicxe.cpu.Instrucao;
import sicxe.io.Carregador;
import sicxe.maquina.ErroExecucao;
import sicxe.maquina.Memoria;
import sicxe.maquina.Palavra;
import sicxe.maquina.Registradores;

// Janela principal do simulador: memoria, registradores, botoes e historico.

// Nada no simulador depende desta classe: ela so le o estado da Memoria,
// dos Registradores e da CPU e manda a CPU dar passos.

// O botao "Rodar" usa javax.swing.Timer e nao um while: o Swing desenha a
// tela na mesma thread que trata os cliques, entao um while congelaria a
// janela ate o fim do programa. Com o Timer a CPU da um passo a cada X ms
// e a tela se redesenha entre um passo e outro.

public class JanelaPrincipal extends JFrame {

    private static final Color COR_ERRO = new Color(215, 40, 40);

    private final Memoria mem;
    private final CPU cpu;
    private final Carregador carregador = new Carregador();

    private final PainelRegistradores painelRegs;
    private final PainelMemoria painelMem;
    private final JTextArea log = new JTextArea();
    private final JLabel rotuloInstrucao = new JLabel(" ");
    private final JLabel rotuloStatus = new JLabel(" ");
    private final Color corStatusPadrao = rotuloStatus.getForeground();
    private final JButton botaoPasso = new JButton("Passo");
    private final JButton botaoRodar = new JButton("Rodar");
    private final JSlider velocidade = new JSlider(1, 500, 120);
    private final Timer relogio;

    private File ultimoArquivo;   // usado pelo Reset para recarregar o programa

    public JanelaPrincipal(Memoria mem, Registradores regs, CPU cpu) {
        super("Simulador SIC/XE - Programacao de Sistemas - UFPel");
        this.mem = mem;
        this.cpu = cpu;
        this.painelRegs = new PainelRegistradores(regs);
        this.painelMem = new PainelMemoria(mem, regs);

        relogio = new Timer(velocidade.getValue(), e -> {
            executarPasso();
            if (cpu.isParada()) {
                pararRelogio();
            }
        });

        montarInterface();
        atualizarTela();
    }

    private void montarInterface() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 720);
        setLocationRelativeTo(null);

        JButton botaoCarregar = new JButton("Carregar programa");
        JButton botaoReset = new JButton("Reset");

        botaoCarregar.addActionListener(e -> escolherArquivo());
        botaoPasso.addActionListener(e -> {
            pararRelogio();
            executarPasso();
        });
        botaoRodar.addActionListener(e -> {
            if (relogio.isRunning()) {
                pararRelogio();
            } else if (!cpu.isParada()) {
                relogio.setDelay(velocidade.getValue());
                relogio.start();
                botaoRodar.setText("Parar");
            }
        });
        botaoReset.addActionListener(e -> reset());
        velocidade.addChangeListener(e -> relogio.setDelay(velocidade.getValue()));

        JPanel barra = new JPanel();
        barra.add(botaoCarregar);
        barra.add(botaoPasso);
        barra.add(botaoRodar);
        barra.add(botaoReset);
        barra.add(Box.createHorizontalStrut(20));
        barra.add(new JLabel("Intervalo (ms):"));
        velocidade.setPreferredSize(new Dimension(160, 30));
        barra.add(velocidade);

        rotuloInstrucao.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
        rotuloInstrucao.setBorder(BorderFactory.createTitledBorder("Ultima instrucao executada"));

        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane rolagemLog = new JScrollPane(log);
        rolagemLog.setBorder(BorderFactory.createTitledBorder("Historico de execucao"));

        JPanel lateral = new JPanel(new BorderLayout());
        lateral.add(painelRegs, BorderLayout.NORTH);
        lateral.add(rolagemLog, BorderLayout.CENTER);
        lateral.setPreferredSize(new Dimension(380, 0));

        JSplitPane divisor = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, painelMem, lateral);
        divisor.setResizeWeight(0.72);

        JPanel rodape = new JPanel(new GridLayout(2, 1));
        rodape.add(rotuloInstrucao);
        rodape.add(rotuloStatus);

        setLayout(new BorderLayout());
        add(barra, BorderLayout.NORTH);
        add(divisor, BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
    }

    // Um passo da CPU, usado tanto pelo botao "Passo" quanto pelo Timer.
    private void executarPasso() {
        if (cpu.isParada()) {
            return;
        }
        Instrucao anterior = cpu.getUltimaInstrucao();
        painelRegs.guardarEstado();   // o que mudar a partir daqui fica verde

        cpu.passo();

        // So registra se a instrucao chegou a ser decodificada. Se deu erro na
        // decodificacao, getUltimaInstrucao() ainda e a anterior. Se deu erro
        // na execucao (ex.: DIV #0), a instrucao aparece e logo depois o erro.
        if (cpu.getUltimaInstrucao() != anterior) {
            registrarNoLog(cpu.getUltimaInstrucao());
        }
        if (cpu.isParada()) {
            log.append(cpu.getMensagem() + "\n");
        }
        atualizarTela();
    }

    private void pararRelogio() {
        relogio.stop();
        botaoRodar.setText("Rodar");
        botaoRodar.setEnabled(!cpu.isParada());
    }

    private void escolherArquivo() {
        pararRelogio();
        File pasta = ultimoArquivo != null ? ultimoArquivo.getParentFile() : new File("testes");
        JFileChooser selecionador = new JFileChooser(pasta);
        if (selecionador.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            carregarArquivo(selecionador.getSelectedFile());
        }
    }

    // Publico para dar para abrir um programa direto pelo codigo (ex.: em testes).
    public void carregarArquivo(File arquivo) {
        pararRelogio();
        try {
            // Carrega primeiro numa memoria de rascunho: se o arquivo tiver erro,
            // a memoria de verdade nao fica com o programa pela metade.
            carregador.carregar(arquivo, new Memoria(mem.getTamanho()));

            mem.limpar();
            Carregador.ResultadoCarga r = carregador.carregar(arquivo, mem);
            mem.limparEscritasRecentes();   // a carga nao conta como "escrita da instrucao"
            cpu.prepararExecucao(r.enderecoExecucao);
            ultimoArquivo = arquivo;

            log.setText("");
            log.append("Programa carregado: " + arquivo.getName() + " ("
                    + r.bytesCarregados + " bytes). PC inicial = "
                    + Palavra.hex(r.enderecoExecucao, 6) + "\n");
            painelRegs.guardarEstado();
            atualizarTela();
        } catch (ErroExecucao e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro ao carregar",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Recarrega o ultimo arquivo, para a memoria voltar ao estado original.
    // Sem arquivo carregado nao faz nada: liberar a CPU aqui faria ela sair
    // executando a memoria vazia (00 00 00 = LDA 0 em modo SIC) sem parar.
    private void reset() {
        pararRelogio();
        if (ultimoArquivo != null) {
            carregarArquivo(ultimoArquivo);
        }
    }

    private void registrarNoLog(Instrucao ins) {
        log.append(Palavra.hex(ins.endereco, 6) + "  "
                + preencher(ins.bytesEmHex(), 8) + "  "
                + cpu.getTextoUltimaInstrucao() + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    private static String preencher(String texto, int tamanho) {
        StringBuilder sb = new StringBuilder(texto);
        while (sb.length() < tamanho) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private void atualizarTela() {
        painelRegs.atualizar();
        painelMem.atualizar();

        String texto = cpu.getTextoUltimaInstrucao();
        rotuloInstrucao.setText("  " + (texto.isEmpty() ? "(nenhuma instrucao executada ainda)" : texto));

        rotuloStatus.setText("  " + cpu.getMensagem()
                + "   |   instrucoes executadas: " + cpu.getContadorInstrucoes());
        rotuloStatus.setForeground(cpu.isErro() ? COR_ERRO : corStatusPadrao);

        botaoPasso.setEnabled(!cpu.isParada());
        botaoRodar.setEnabled(!cpu.isParada() || relogio.isRunning());
    }
}
