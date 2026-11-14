. Erro esperado: macros, linha 3: falta o MEND da macro DOBRA
ERRO1    START   0
DOBRA    MACRO   &V
         LDA     &V
         ADD     &V
         STA     &V
         RSUB
VALOR    WORD    1
         END     ERRO1
