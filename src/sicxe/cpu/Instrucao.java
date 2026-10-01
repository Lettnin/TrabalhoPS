package sicxe.cpu;

import sicxe.maquina.Palavra;

public class Instrucao {

    public int endereco;      
    public int tamanho;       
    public int formato;       
    public int opcode;       
    public String mnemonico;  

 
    public int r1;
    public int r2;


    public boolean n, i, x, b, p, e;
    public int disp;          
    public boolean modoSIC;   

    public int[] bytesBrutos;

    public String bytesEmHex() {
        StringBuilder sb = new StringBuilder();
        for (int by : bytesBrutos) {
            sb.append(Palavra.hex(by, 2));
        }
        return sb.toString();
    }
}