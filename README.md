# Magic Interpreter 🔮
A simple interpreter witten in Java for a toy language. This is custom interpretation of the series of articles titled ["Let’s Build A Simple Interpreter"](https://ruslanspivak.com/lsbasi-part1/) by Ruslan Spivak in his blog.
 
 At the moment, the Interpreter can accurately identify and label different tokens inside a `.toy` file. Some of the tokens it can identify include, but are not limited to:
  - Keywords: `let`, `const`, `func`, `if`, `elif`, `else`, + more.
  - Static Types: `String`, `Int`, `Real`, `void`, `None`, + more.
  - Literals: `"String Sequences"`, `2342`, `3.1242`, `true`, `false`, `none`, + more.
  - Arithmetic Operators: `+`, `-`, `*`, `/`, `**`, `%`, and `mod`.
  - Logical Operators: `<`, `>`, `==`, `equals`, `!`, `not`, + more.
  - Delimiters & Separators: `()`, `,`, `{}`, `:`, `[]`, `.`, and `;`
  
The next step for this project is to implement a Parser (currently on the works), which will take the generated tokens and compose an Abstract Syntax Tree that an interpreter can then read.
   
## The General Idea
The general idea of an interpreter implementation is as follows:

1. The programmer writes a program in a source file.
2. The contents of the source file are read and inputted into a [Lexer](https://github.com/faustotnc/Interpreter/tree/master/Lexer) that converts the input text into a list of [Tokens](https://github.com/faustotnc/Interpreter/tree/master/Tokens).
3. The tokens are then fed into a [Parser](https://github.com/faustotnc/Interpreter/tree/master/Parser) which generates an [Abstract Syntax Tree (AST)](https://github.com/faustotnc/Interpreter/tree/master/AbstractSyntaxTree). The AST is a type of abstract data structure that represents the syntactic anatomy of the source code.
4. Finally, an [Interpreter](https://github.com/faustotnc/Interpreter/tree/master/Interpreter) takes the AST and "visits" each node and leaf of the AST until it has completed executing the source code.