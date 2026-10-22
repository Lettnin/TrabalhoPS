. Teste 1 do montador - o mesmo programa de testes/01-soma.hex,
. agora escrito em assembly. Tem que gerar exatamente os mesmos bytes.
SOMA     START   0
         LDA     #5          . A = 5
         ADD     #3          . A = 8
         STA     256         . memoria[0100] = 8 (256 = 0100 em hexadecimal)
         RSUB                . L = 0 -> fim do programa
         END     SOMA
