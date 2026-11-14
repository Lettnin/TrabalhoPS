. Erro esperado: montador (MASMAPRG.ASM), linha 20: simbolo duplicado: POS
. (ja definido na linha 12). E o teste 2 sem o .SER: as duas expansoes
. criam o rotulo POS. Quem acusa e o montador, no fonte expandido.
ERRO5    START   0
DIFABS   MACRO   &A,&B,&C
         LDA     &A
         SUB     &B
         COMP    #0
         JGT     POS
         LDA     &B
         SUB     &A
POS      STA     &C
         MEND
         DIFABS  V1,V2,D1
         DIFABS  V3,V4,D2
         RSUB
V1       WORD    10
V2       WORD    3
V3       WORD    4
V4       WORD    20
D1       RESW    1
D2       RESW    1
         END     ERRO5
