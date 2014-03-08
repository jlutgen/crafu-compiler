header 
{
    package decaf;
}

options 
{
  mangleLiteralPrefix = "TK_";
  language="Java";
}

class DecafScanner extends Lexer;
options 
{
  k=2;
}

tokens 
{
    "boolean";
    "break"; 
    "callout"; 
    "class";
    "continue"; 
    "else"; 
    "for"; 
    "if";
    "int";
    "return"; 
    "void";
    TRUE="true";
    FALSE="false";
}

LCURLY options { paraphrase = "{"; } : "{";
RCURLY options { paraphrase = "}"; } : "}";
LSQUARE options { paraphrase = "["; } : "[";
RSQUARE options { paraphrase = "]"; } : "]";
LPAREN options { paraphrase = "("; } : "(";
RPAREN options { paraphrase = ")"; } : ")";
COMMA options { paraphrase = ","; } : ",";
SEMI options { paraphrase = ";"; } : ";";
LESS options { paraphrase = "<"; } : "<";
GREATER  options { paraphrase = ">"; } : ">";
LESSEQ options { paraphrase = "<="; } : "<=";
GREATEREQ options { paraphrase = ">="; } : ">=";
EQUAL options { paraphrase = "=="; } : "==";
NOTEQUAL options { paraphrase = "!="; } : "!=";
ASSIGN options { paraphrase = "="; } : "=";
MINUSASSIGN options { paraphrase = "-="; } : "-=";
PLUSASSIGN options { paraphrase = "+="; } : "+=";
AND options { paraphrase = "&&"; } : "&&";
OR options { paraphrase = "||"; } : "||";
NOT options { paraphrase = "!"; } : "!";
MINUS options { paraphrase = "-"; } : "-";
PLUS options { paraphrase = "+"; } : "+";
TIMES options { paraphrase = "*"; } : "*";
DIV options { paraphrase = "/"; } : "/";
MOD options { paraphrase = "%"; } : "%";

ID options { paraphrase = "an identifier"; } : 
    ALPHA (ALPHANUM)*;

WS_ : (' ' | '\t' | '\n' {newline();}) {_ttype = Token.SKIP; };

SL_COMMENT : "//" (~'\n')* '\n' {_ttype = Token.SKIP; newline (); };

CHAR : '\'' CH '\'';
STRING : '"' (CH)* '"';        

HEX : "0x" (HEXDIGIT)+;
DECIMAL: (DIGIT)+;

protected
CH : ESC | ~('\n' | '\t' | '\'' | '\\' | '"');
protected
ESC :  '\\' ('n'|'"'|'t'|'\\'|'\'');
protected
ALPHA : ('a'..'z' | 'A'..'Z' | '_');
protected
DIGIT : '0'..'9';
protected
ALPHANUM : ALPHA | DIGIT;
protected
HEXDIGIT : DIGIT | 'a'..'f' | 'A'..'F';
