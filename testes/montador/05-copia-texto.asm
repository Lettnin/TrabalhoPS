. Teste 5 do montador - so existe na Etapa 2. Usa o que os outros 4 nao usam:
. EQU com * e com expressao, RESB com simbolo, BYTE C'...' e X'...', WORD
. negativo, TIX e formato 4 com numero (que nao gera registro M).
. Copia o texto de ORIGEM para DESTINO, um byte por vez.
COPIA    START   0
         LDX     #0          . X = 0
LOOP     LDCH    ORIGEM,X    . A recebe o byte X do texto
         STCH    DESTINO,X   . e grava no destino
         TIX     #TAM        . X = X + 1 e compara X com TAM
         JLT     LOOP        . repete enquanto X < TAM
         LDA     #TAM
         STA     COPIADOS    . COPIADOS = 15
         +LDT    #4096       . formato 4 com numero: T = 4096 (001000)
         CLEAR   L
         RSUB
ORIGEM   BYTE    C'SIC/XE NA UFPEL'
FIMORIG  EQU     *           . * = endereco atual, logo depois do texto
TAM      EQU     FIMORIG-ORIGEM
DESTINO  RESB    TAM
COPIADOS RESW    1
MASCARA  BYTE    X'F0F0F0'
NEGATIVO WORD    -1
         END     COPIA
