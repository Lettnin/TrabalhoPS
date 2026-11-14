. Erro esperado: macros, linha 8: parametros demais na chamada de SOMA (ela tem 3)
ERRO3    START   0
SOMA     MACRO   &A,&B,&DEST
         LDA     &A
         ADD     &B
         STA     &DEST
         MEND
         SOMA    V1,V2,V3,V4
         RSUB
V1       WORD    1
V2       WORD    2
V3       RESW    1
V4       RESW    1
         END     ERRO3
