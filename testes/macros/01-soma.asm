. Teste 1 das macros - a macro mais simples, com parametros posicionais.
. SOMA guarda em &DEST a soma de &A com &B. Ela e chamada duas vezes:
. cada chamada vira tres linhas, com os parametros reais no lugar dos
. formais. A segunda chamada tem rotulo, que vira "DOBRAR EQU *".
TESTE1   START   0
SOMA     MACRO   &A,&B,&DEST
         LDA     &A
         ADD     &B
         STA     &DEST
         MEND
         SOMA    PRECO,FRETE,TOTAL    . TOTAL = 5 + 7 = 12
DOBRAR   SOMA    TOTAL,TOTAL,DOBRO    . DOBRO = 12 + 12 = 24
         RSUB
PRECO    WORD    5
FRETE    WORD    7
TOTAL    RESW    1
DOBRO    RESW    1
         END     TESTE1
