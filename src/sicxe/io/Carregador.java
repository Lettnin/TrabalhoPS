package sicxe.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;



import sicxe.maquina.ErroExecucao;
import sicxe.maquina.Memoria;


public class Carregador {

    public static class ResultadoCarga {
        public String nomePrograma = "";
        public int enderecoInicial = 0;   // onde o programa foi colocado
        public int enderecoExecucao = 0;  // onde a execucao comeca
        public int bytesCarregados = 0;
    }

}