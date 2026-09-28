package sicxe.maquina;

  // Utilitarios para trabalhar com a palavra de 24 bits do SIC/XE.
 
  // O Java nao tem tipo de 24 bits. Guardamos tudo em "int" (32 bits) e,
  // depois de qualquer conta, aplicamos mascarar() para descartar os bits
  // que passaram de 24. E como nao existe inteiro sem sinal em Java,
  // paraSinalizado() converte a palavra de 24 bits (complemento de 2)
  // para o valor com sinal que o Java entende.
  
public final class Palavra {

    public static final int MASCARA_24 = 0xFFFFFF;   // 24 bits ligados
    public static final int BIT_SINAL_24 = 0x800000; // bit 23 (o mais alto)

    private Palavra() { }

    // Descarta tudo que passou de 24 bits.
    public static int mascarar(int valor) {
        return valor & MASCARA_24;
    }

    // Converte 24 bits em complemento de 2 para inteiro com sinal do Java.
    public static int paraSinalizado(int valor24) {
        int v = mascarar(valor24);
        if ((v & BIT_SINAL_24) != 0) {
            return v - 0x1000000;
        }
        return v;
    }

    // Converte um deslocamento de 12 bits (complemento de 2) para valor com sinal. 
    public static int sinal12(int disp) {
        int d = disp & 0xFFF;
        if ((d & 0x800) != 0) {
            return d - 0x1000;
        }
        return d;
    }

    // Formata um valor em hexadecimal maiusculo com quantidade fixa de digitos.
    public static String hex(int valor, int digitos) {
        String s = Integer.toHexString(valor).toUpperCase();
        while (s.length() < digitos) {
            s = "0" + s;
        }
        if (s.length() > digitos) {
            s = s.substring(s.length() - digitos);
        }
        return s;
    }
}
