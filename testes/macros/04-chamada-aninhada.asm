. Teste 4 das macros - chamada aninhada (exemplo DISCR dos slides).
. DELTA calcula o discriminante B*B - 4*A*C chamando a macro MULT tres
. vezes. MULT e definida DEPOIS de DELTA: tudo bem, porque as linhas de
. DELTA so sao lidas quando DELTA e chamada, e ai MULT ja existe.
. O #4 mostra que o parametro real pode trazer o modo de enderecamento.
TESTE4   START   0
DELTA    MACRO   &A,&B,&C,&D
         MULT    &A,&C,TEMP1        . TEMP1 = A * C
         MULT    TEMP1,#4,TEMP1     . TEMP1 = 4 * A * C
         MULT    &B,&B,TEMP2        . TEMP2 = B * B
         LDA     TEMP2
         SUB     TEMP1
         STA     &D                 . D = B*B - 4*A*C
         MEND
MULT     MACRO   &X,&Y,&Z
         LDA     &X
         MUL     &Y
         STA     &Z
         MEND
         DELTA   A1,B1,C1,D1        . 7*7 - 4*2*3 = 25
         DELTA   A2,B2,C2,D2        . 2*2 - 4*1*5 = -16
         RSUB
A1       WORD    2
B1       WORD    7
C1       WORD    3
A2       WORD    1
B2       WORD    2
C2       WORD    5
D1       RESW    1
D2       RESW    1
TEMP1    RESW    1
TEMP2    RESW    1
         END     TESTE4
