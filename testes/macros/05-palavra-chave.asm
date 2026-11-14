. Teste 5 das macros - notacao dos slides (MCDEFN e MCEND), parametros por
. palavra-chave, valor padrao e parametro omitido.
. INCR soma (ou subtrai) um passo a uma variavel. &PASSO vale 1 e &OP vale
. ADD quando a chamada nao diz nada. O &OP ocupa o lugar da operacao: o
. parametro pode ser qualquer pedaco da linha.
TESTE5   START   0
INCR     MCDEFN  &VAR,&PASSO=1,&OP=ADD
         LDA     &VAR
         &OP     #&PASSO
         STA     &VAR
         MCEND
         INCR    CONTA                     . CONTA = 10 + 1 = 11 (os dois padroes)
         INCR    CONTA,5                   . CONTA = 11 + 5 = 16
         INCR    OP=SUB,VAR=CONTA          . CONTA = 16 - 1 = 15 (palavra-chave)
         INCR    CONTA,,SUB                . CONTA = 15 - 1 = 14 (passo omitido)
         INCR    OUTRA,OP=SUB,PASSO=3      . OUTRA = 100 - 3 = 97 (misturado)
         RSUB
CONTA    WORD    10
OUTRA    WORD    100
         END     TESTE5
