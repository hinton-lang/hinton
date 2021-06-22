use crate::ast::*;
use crate::bytecode;
use crate::bytecode::OpCode;
use crate::compiler::symbols::{Symbol, SymbolTable, SymbolType};
use crate::compiler::{Compiler, CompilerType, FunctionScope, UpValue};
use crate::errors::CompilerErrorType;
use crate::lexer::tokens::Token;
use crate::objects::{FuncObject, Object};
use std::cell::RefCell;
use std::rc::Rc;

impl Compiler {
   /// Compiles a function declaration statement.
   pub(super) fn compile_function_decl(&mut self, decl: &FunctionDeclNode) {
      if let Ok(parent_symbol_pos) = self.declare_symbol(&decl.name, SymbolType::Function) {
         let func_pos = (decl.name.line_num, decl.name.column_num);
         let prev_compiler_type = std::mem::replace(&mut self.compiler_type, CompilerType::Function);

         // The first element in a symbol table is always the symbol representing
         // the function to which the symbol table belongs.
         let symbols = SymbolTable::new(vec![Symbol {
            name: decl.name.lexeme.clone(),
            s_type: SymbolType::Function,
            is_initialized: true,
            depth: 0,
            is_used: true,
            line_info: func_pos,
            is_captured: false,
         }]);

         // Make this function declaration the current function scope.
         self.functions.push(FunctionScope {
            function: FuncObject {
               defaults: vec![],
               min_arity: decl.arity.0,
               max_arity: decl.arity.1,
               chunk: bytecode::Chunk::new(),
               name: decl.name.lexeme.clone(),
               up_val_count: 0,
            },
            s_table: symbols,
            scope_depth: 0,
            loops: vec![],
            breaks: vec![],
            up_values: vec![],
         });

         // Add the function's name to the pool of the function
         self.add_literal_to_pool(Object::String(decl.name.lexeme.clone()), &decl.name, false);
         // compiles the parameter declarations so that the compiler knows about their lexical
         // scoping (their stack position).
         self.compile_parameters(&decl.params);

         // Compile the function's body
         if decl.body.len() == 0 {
            self.emit_return(&None, func_pos)
         } else {
            for (index, node) in decl.body.iter().enumerate() {
               self.compile_node(node);

               // Emit an implicit `return` if the body does not end with a return.
               if index == decl.body.len() - 1 {
                  match node {
                     &ASTNode::ReturnStmt(_) => {}
                     _ => self.emit_return(&None, func_pos),
                  }
               };
            }
         }

         // Show a warning about unused symbols in the function body.
         self.current_func_scope_mut().s_table.pop_scope(0, true, true);

         // When the 'show_bytecode' features flag is on, keep track of the
         // previous function's up_values so that we can pretty-print the
         // up_values captured by the closure.
         #[cfg(feature = "show_bytecode")]
         self.print_pretty_bytecode();
         #[cfg(feature = "show_raw_bytecode")]
         self.print_raw_bytecode();

         // Takes the generated function object.
         let function = std::mem::take(&mut self.current_func_scope_mut().function);
         // Takes the up_values generated by the compiled function.
         let up_values = std::mem::take(&mut self.current_func_scope_mut().up_values);

         // Go back to the previous function.
         self.functions.pop();
         self.compiler_type = prev_compiler_type;

         // Loads the function object onto the stack at runtime.
         self.emit_function(function, up_values, &decl.name);

         // Compile the named parameters so that they can be
         // bound to the function at runtime.
         if decl.arity.0 != decl.arity.1 {
            self.bind_default_params(decl);
         }

         // If we are in the global scope, declarations are
         // stored in the VM.globals hashmap.
         if self.is_global_scope() {
            self.define_as_global(&decl.name);
            self.globals.mark_initialized(parent_symbol_pos);
         } else {
            // Marks the variables as initialized
            // a.k.a, defines the variables.
            self
               .current_func_scope_mut()
               .s_table
               .mark_initialized(parent_symbol_pos);
         }
      }
   }

   /// Emits the appropriate code to either load a function object from the constant or create
   // a closure at runtime.
   ///
   /// # Parameters
   /// - `function`: The function object to be loaded.
   /// - `up_values`: The UpValues of this function.
   /// - `token`: A reference to the function's token.
   fn emit_function(&mut self, function: FuncObject, up_values: Vec<UpValue>, token: &Token) {
      let func = Object::Function(Rc::new(RefCell::new(function)));

      // If the function does not close over any values, then there is
      // no need to create a closure object at runtime.
      if up_values.len() == 0 {
         self.add_literal_to_pool(func, token, true);
         return;
      }

      let func_pos = (token.line_num, token.column_num);

      // Add the function object to the literal pool of the parent function
      if let Some(idx) = self.add_literal_to_pool(func, token, false) {
         if idx < 256 {
            if up_values.len() < 256 {
               self.emit_op_code_with_byte(OpCode::MakeClosure, idx as u8, func_pos);

               for up in up_values {
                  self.emit_raw_byte(if up.is_local { 1u8 } else { 0u8 }, func_pos);
                  self.emit_raw_byte(up.index as u8, func_pos);
               }
            } else {
               self.emit_op_code_with_byte(OpCode::MakeClosureLarge, idx as u8, func_pos);

               for up in up_values {
                  self.emit_raw_byte(if up.is_local { 1u8 } else { 0u8 }, func_pos);
                  self.emit_raw_short(up.index as u16, func_pos);
               }
            }
         } else {
            if up_values.len() < 256 {
               self.emit_op_code_with_short(OpCode::MakeClosureLong, idx, func_pos);

               for up in up_values {
                  self.emit_raw_byte(if up.is_local { 1u8 } else { 0u8 }, func_pos);
                  self.emit_raw_byte(up.index as u8, func_pos);
               }
            } else {
               self.emit_op_code_with_short(OpCode::MakeClosureLongLarge, idx, func_pos);

               for up in up_values {
                  self.emit_raw_byte(if up.is_local { 1u8 } else { 0u8 }, func_pos);
                  self.emit_raw_short(up.index as u16, func_pos);
               }
            }
         }
      }
   }

   /// Emits bytecode to bind the default values for the named parameters of a function.
   ///
   /// # Parameters
   // * `decl`: The function declaration node where these named parameters were declared.
   fn bind_default_params(&mut self, decl: &FunctionDeclNode) {
      // Compiles the named parameters so that they can be on top
      // of the stack when the function gets composed at runtime.
      for param in &decl.params {
         match &param.default {
            Some(expr) => {
               self.compile_node(&expr);
            }
            None => {
               if param.is_optional {
                  self.emit_op_code(OpCode::LoadImmNull, (param.name.line_num, param.name.column_num));
               }
            }
         }
      }

      // Once all the named parameter expressions are compiled, we bind
      // each of the named parameters to the function
      self.emit_op_code_with_byte(
         OpCode::BindDefaults,
         (decl.arity.1 - decl.arity.0) as u8,
         (decl.name.line_num, decl.name.column_num),
      );
   }

   /// Compiles the parameter declaration statements of a function.
   pub(super) fn compile_parameters(&mut self, params: &Vec<Parameter>) {
      for param in params.iter() {
         match self.declare_symbol(&param.name, SymbolType::Parameter) {
            // Do nothing after the parameter has been declared. Default
            // values will be compiled by the function's parent scope.
            Ok(_) => {}
            // We do nothing if there was an error because the `declare_symbol()`
            // function takes care of reporting the appropriate error for us.
            // Explicit `return` to stop the loop.
            Err(_) => return,
         }
      }
   }

   /// Compiles a return statement.
   pub(super) fn compile_return_stmt(&mut self, stmt: &ReturnStmtNode) {
      if let CompilerType::Script = self.compiler_type {
         self.error_at_token(
            &stmt.token,
            CompilerErrorType::Syntax,
            "Cannot return outside of function.",
         );
         return;
      }

      self.emit_return(&stmt.value, (stmt.token.line_num, stmt.token.column_num))
   }

   /// Emits bytecode to return out of a function at runtime.
   ///
   /// # Parameters
   /// - `value` (Option) – The AST node of the return expression (if any).
   /// - `token_pos`: The position of the return statement in the source code.
   fn emit_return(&mut self, value: &Option<Box<ASTNode>>, token_pos: (usize, usize)) {
      if let Some(node) = value {
         self.compile_node(node);
      } else {
         self.emit_op_code(OpCode::LoadImmNull, token_pos);
      }

      let depth = self.relative_scope_depth();

      let symbols = self
         .current_func_scope_mut()
         .s_table
         .pop_scope(depth, false, false);

      for (i, is_captured) in symbols.iter().rev().enumerate() {
         if *is_captured {
            if i < 256 {
               self.emit_op_code_with_byte(OpCode::CloseUpVal, i as u8, token_pos)
            } else {
               self.emit_op_code_with_short(OpCode::CloseUpValLong, i as u16, token_pos);
            }
         }
      }

      self.emit_op_code(OpCode::Return, token_pos);
   }
}
