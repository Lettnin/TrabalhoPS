package sicxe.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import sicxe.maquina.Memoria;
import sicxe.maquina.Palavra;
import sicxe.maquina.Registradores;

// Visao hexadecimal da memoria, 16 bytes por linha, mais a coluna ASCII.

// O modelo da tabela le DIRETO da Memoria a cada desenho, sem copiar para um
// vetor a parte: assim a tela nunca mostra informacao velha.
// Pinta de amarelo o byte onde o PC esta e de verde os bytes escritos
// pela ultima instrucao executada (Memoria.foiEscritoRecentemente).

public class PainelMemoria extends JPanel {

    private static final int BYTES_POR_LINHA = 16;
    private static final Color COR_PC = new Color(255, 236, 145);
    private static final Color COR_ESCRITA = new Color(178, 233, 178);

    private final Memoria mem;
    private final Registradores regs;
    private final Modelo modelo = new Modelo();
    private final JTable tabela = new JTable(modelo);

    public PainelMemoria(Memoria mem, Registradores regs) {
        this.mem = mem;
        this.regs = regs;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Memoria"));

        // fonte monoespacada, senao os numeros nao alinham
        tabela.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        tabela.getTableHeader().setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        tabela.getTableHeader().setReorderingAllowed(false);
        tabela.setRowHeight(20);
        tabela.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabela.setCellSelectionEnabled(true);

        tabela.getColumnModel().getColumn(0).setPreferredWidth(70);
        for (int c = 1; c <= BYTES_POR_LINHA; c++) {
            tabela.getColumnModel().getColumn(c).setPreferredWidth(30);
        }
        tabela.getColumnModel().getColumn(BYTES_POR_LINHA + 1).setPreferredWidth(170);

        tabela.setDefaultRenderer(Object.class, new Pintor());
        add(new JScrollPane(tabela), BorderLayout.CENTER);
    }

    // Redesenha e rola ate a linha do PC, para nao perder a execucao de vista.
    public void atualizar() {
        modelo.fireTableDataChanged();
        irPara(regs.getPC());
    }

    // Rola a tabela para deixar a linha do endereco no meio da tela.
    // So rola se a linha estiver fora da tela ou encostada na borda, para a
    // tabela nao ficar pulando a cada passo enquanto o PC anda na mesma regiao.
    public void irPara(int endereco) {
        int linha = endereco / BYTES_POR_LINHA;
        if (linha < 0 || linha >= modelo.getRowCount()) {
            return;
        }
        JViewport janela = (JViewport) tabela.getParent();
        Rectangle visivel = janela.getViewRect();
        Rectangle celula = tabela.getCellRect(linha, 0, true);
        int margem = 3 * tabela.getRowHeight();
        if (celula.y >= visivel.y + margem
                && celula.y + celula.height <= visivel.y + visivel.height - margem) {
            return;
        }
        int y = celula.y - (visivel.height - celula.height) / 2;
        y = Math.min(y, tabela.getHeight() - visivel.height);
        y = Math.max(y, 0);
        janela.setViewPosition(new Point(visivel.x, y));
    }

    private class Modelo extends AbstractTableModel {

        public int getRowCount() { return mem.getTamanho() / BYTES_POR_LINHA; }
        public int getColumnCount() { return BYTES_POR_LINHA + 2; }

        public String getColumnName(int c) {
            if (c == 0) return "Endereco";
            if (c == BYTES_POR_LINHA + 1) return "ASCII";
            return Palavra.hex(c - 1, 1);
        }

        public Object getValueAt(int linha, int coluna) {
            int base = linha * BYTES_POR_LINHA;
            if (coluna == 0) {
                return Palavra.hex(base, 6);
            }
            if (coluna == BYTES_POR_LINHA + 1) {
                StringBuilder sb = new StringBuilder();
                for (int k = 0; k < BYTES_POR_LINHA; k++) {
                    int v = mem.lerByte(base + k);
                    sb.append(v >= 32 && v < 127 ? (char) v : '.');
                }
                return sb.toString();
            }
            return Palavra.hex(mem.lerByte(base + coluna - 1), 2);
        }
    }

    private class Pintor extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object valor,
                boolean selecionado, boolean comFoco, int linha, int coluna) {
            Component c = super.getTableCellRendererComponent(t, valor, selecionado,
                    comFoco, linha, coluna);
            setHorizontalAlignment(coluna == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);

            Color fundo = null;
            if (coluna >= 1 && coluna <= BYTES_POR_LINHA) {
                int endereco = linha * BYTES_POR_LINHA + (coluna - 1);
                if (mem.foiEscritoRecentemente(endereco)) {
                    fundo = COR_ESCRITA;
                } else if (endereco == regs.getPC()) {
                    fundo = COR_PC;
                }
            }
            if (selecionado) {
                c.setBackground(t.getSelectionBackground());
                c.setForeground(t.getSelectionForeground());
            } else if (fundo != null) {
                // texto preto no destaque, senao some quando o sistema usa tema escuro
                c.setBackground(fundo);
                c.setForeground(Color.BLACK);
            } else {
                c.setBackground(t.getBackground());
                c.setForeground(t.getForeground());
            }
            return c;
        }
    }
}
