package sicxe.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import sicxe.maquina.Palavra;
import sicxe.maquina.Registradores;

// Tabela com o valor atual de cada registrador, em hexadecimal e em decimal com sinal.

// Pinta de verde as linhas dos registradores que mudaram desde o ultimo
// guardarEstado(). A janela chama guardarEstado() logo antes de cada passo
// da CPU, entao o verde mostra exatamente o que a ultima instrucao alterou.

public class PainelRegistradores extends JPanel {

    // O F (ponto flutuante) nao aparece porque nenhuma instrucao implementada usa ele.
    private static final int[] ORDEM = {
        Registradores.A, Registradores.X, Registradores.L, Registradores.B,
        Registradores.S, Registradores.T, Registradores.PC, Registradores.SW
    };

    private static final Color COR_ALTERADO = new Color(178, 233, 178);

    private final Registradores regs;
    private final Modelo modelo = new Modelo();
    private final JTable tabela = new JTable(modelo);
    private final int[] valoresAnteriores = new int[ORDEM.length];

    public PainelRegistradores(Registradores regs) {
        this.regs = regs;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Registradores"));

        tabela.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        tabela.setRowHeight(22);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(40);
        tabela.setDefaultRenderer(Object.class, new Pintor());

        JScrollPane rolagem = new JScrollPane(tabela);
        // altura suficiente para as 8 linhas sem barra de rolagem
        rolagem.setPreferredSize(new Dimension(0,
                tabela.getRowHeight() * (ORDEM.length + 1) + 8));
        add(rolagem, BorderLayout.CENTER);

        guardarEstado();
    }

    // Tira um retrato dos valores atuais. O que mudar depois disso fica verde.
    public void guardarEstado() {
        for (int k = 0; k < ORDEM.length; k++) {
            valoresAnteriores[k] = regs.get(ORDEM[k]);
        }
    }

    public void atualizar() {
        modelo.fireTableDataChanged();
    }

    private boolean mudou(int linha) {
        return regs.get(ORDEM[linha]) != valoresAnteriores[linha];
    }

    private class Modelo extends AbstractTableModel {
        private final String[] colunas = { "Reg", "Hex", "Decimal" };

        public int getRowCount() { return ORDEM.length; }
        public int getColumnCount() { return colunas.length; }
        public String getColumnName(int c) { return colunas[c]; }

        public Object getValueAt(int linha, int coluna) {
            int numero = ORDEM[linha];
            int valor = regs.get(numero);
            switch (coluna) {
                case 0:
                    return Registradores.nome(numero);
                case 1:
                    return Palavra.hex(valor, 6);
                case 2:
                    if (numero == Registradores.SW) {
                        return valor + "  (CC " + regs.getCCTexto() + ")";
                    }
                    return String.valueOf(Palavra.paraSinalizado(valor));
                default:
                    return "";
            }
        }
    }

    private class Pintor extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object valor,
                boolean selecionado, boolean comFoco, int linha, int coluna) {
            Component c = super.getTableCellRendererComponent(t, valor, selecionado,
                    comFoco, linha, coluna);
            if (selecionado) {
                c.setBackground(t.getSelectionBackground());
                c.setForeground(t.getSelectionForeground());
            } else if (mudou(linha)) {
                // texto preto no destaque, senao some quando o sistema usa tema escuro
                c.setBackground(COR_ALTERADO);
                c.setForeground(Color.BLACK);
            } else {
                c.setBackground(t.getBackground());
                c.setForeground(t.getForeground());
            }
            return c;
        }
    }
}
