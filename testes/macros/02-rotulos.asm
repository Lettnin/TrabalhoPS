. Teste 2 das macros - rotulo dentro da macro, com .SER.
. DIFABS guarda em &C a diferenca absoluta |&A - &B| (exemplo ABSDIF dos
. slides). O rotulo POS aparece em toda expansao; sem o .SER, a segunda
. chamada criaria um segundo POS e o montador acusaria simbolo duplicado
. (veja erros/sem-ser.asm). Com o .SER, cada expansao ganha o seu numero:
. POS0001 na primeira, POS0002 na segunda.
TESTE2   START   0
DIFABS   MACRO   &A,&B,&C
         LDA     &A
         SUB     &B
         COMP    #0
         JGT     POS.SER       . se A - B > 0, ja e o resultado
         LDA     &B
         SUB     &A            . senao, o resultado e B - A
POS.SER  STA     &C
         MEND
         DIFABS  V1,V2,D1      . D1 = |10 - 3| = 7
         DIFABS  V3,V4,D2      . D2 = |4 - 20| = 16
         RSUB
V1       WORD    10
V2       WORD    3
V3       WORD    4
V4       WORD    20
D1       RESW    1
D2       RESW    1
         END     TESTE2
