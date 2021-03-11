; Syntax Spec:   https://tools.ietf.org/html/rfc5234
; Parser tester: https://tools.ietf.org/tools/bap/
; https://mdkrajnak.github.io/ebnftest/
; https://www.bottlecaps.de/rr/ui

action = cond source [act]

cond = condP condG

condP = ["~"]

condG = [(condT / condF)]

condT = "?"

condF = "!"

source = rType / vType / uType

rType = ( rIn / rOut ) [rMG eoM]

rIn = rInH / rInB

rInH = "head" ["[" 1*rInHN "]"]

rInHN = anyChar

rInB = "body"

rOut = rOutH / rOutB

rOutH = "head" ["[" 1*rOutHN "]"]

rOutHN = anyChar

rOutB = "body"

rMG = ":{" rM "}"

rM = *CHAR

vType = [vLoc] 
    "var" ["[" vN "]"]
    [vM eoM]
    
vLoc = [vS / vB]

vS = "&"

vB = "b"

eoM = ("->" / "\s" / "$")

vN = [ ALPHA ] *CHAR

vM = ":{" 1*CHAR "}"

uType = "use" [ ":{" *uM "}" ]

uM = DIGIT / "=" / "," / "." / "<" / ">"

act = "->" ( agMG / agVG )

agMG = "{" aM "}" eoA

eoA = ("\s" / "$")

aM = *CHAR

agVG = ["&"] aVN

aVN = ALPHA *anyChar

anyChar = (ALPHA / DIGIT / "_")

ALPHA = %x41-5A / %x61-7A  ; A-Z / a-z

DIGIT = %x30-39  ; 0-9

CHAR =  %x01-7F  ; any 7-bit US-ASCII character,
                 ;  excluding NUL
