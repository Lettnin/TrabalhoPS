package sicxe;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import sicxe.cpu.CPU;
import sicxe.gui.JanelaPrincipal;
import sicxe.maquina.Memoria;
import sicxe.maquina.Registradores;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignorado) {
            }
            Memoria memoria = new Memoria(Memoria.TAMANHO_PADRAO);
            Registradores registradores = new Registradores();
            CPU cpu = new CPU(memoria, registradores);
            new JanelaPrincipal(memoria, registradores, cpu).setVisible(true);
        });
    }
}