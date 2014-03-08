header {package decaf;}

options
{
  mangleLiteralPrefix = "TK_";
  language="Java";
}

class DecafParser extends Parser;
options
{
  importVocab=DecafScanner;
  k=3;
  buildAST=true;
}

program: TK_class ID LCURLY (field_decl)* (method_decl)* RCURLY EOF;

field_decl: type ID (array)? (COMMA ID (array)?)* SEMI;

method_decl: (type | TK_void) ID LPAREN ( type ID (COMMA type ID)* )? RPAREN block;

block: LCURLY (var_decl)* (statement)* RCURLY;

var_decl: type ID (COMMA ID)* SEMI;

statement:
    location assign_op expr SEMI
    | method_call SEMI
    | TK_if LPAREN expr RPAREN block (TK_else block)?
    | TK_for ID ASSIGN expr COMMA expr block
    | TK_return (expr)? SEMI
    | TK_break SEMI
    | TK_continue SEMI
    | block
    ;

assign_op: ASSIGN | MINUSASSIGN | PLUSASSIGN;

method_call: 
    method_name LPAREN ( expr (COMMA expr)* )? RPAREN 
    | TK_callout LPAREN string_literal (COMMA callout_arg (COMMA callout_arg)* )? RPAREN
    ;

method_name: ID;

location: 
    ID
    | ID LSQUARE expr RSQUARE
    ;

expr: expr1;
expr1: expr2 (OR expr2)*;
expr2: expr3 (AND expr3)*;
expr3: expr4 (eq_op expr4)*;
expr4: expr5 (rel_op expr5)*;
expr5: expr6 ( (PLUS | MINUS) expr6 )*;
expr6: expr7 ( (TIMES | DIV | MOD) expr7)*;
expr7: (NOT)? expr8;
expr8: 
    location
    | method_call
    | literal
    | LPAREN expr RPAREN
    | MINUS expr8;
    
callout_arg: expr | string_literal;

bin_op: arith_op | rel_op | eq_op | cond_op;

arith_op: PLUS | MINUS | TIMES | DIV | MOD;

rel_op: LESS | LESSEQ | GREATER | GREATEREQ;

eq_op: EQUAL | NOTEQUAL;

cond_op: AND | OR;

literal: int_literal | char_literal | bool_literal;

int_literal: HEX | DECIMAL;

bool_literal: TRUE | FALSE;

char_literal: CHAR;

string_literal: STRING;

type: TK_int | TK_boolean;

array: LSQUARE int_literal RSQUARE;
