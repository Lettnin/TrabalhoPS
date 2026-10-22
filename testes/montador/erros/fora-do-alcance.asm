. Erro esperado: linha 4: o endereco 000BBE esta longe demais para o PC
. (e nao ha BASE); use o formato 4 (+)
ERRO4    START   0
         LDA     LONGE
         RSUB
ESPACO   RESB    3000
LONGE    WORD    5
         END     ERRO4
