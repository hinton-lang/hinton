use std::rc::Rc;

use super::{tokens::Token, tokens::TokenType::*, Lexer};

impl<'a> Lexer {
    /// Makes a numeric literal. This includes Binary, Octal, Decimal,
    /// Floating-Point, and Hexadecimal numbers.
    ///
    /// ## Returns
    /// * `Token` – A numeric token (integer, float, binary, octal, and hex).
    pub fn make_numeric_token(&mut self) -> Rc<Token> {
        // Support for hexadecimal integers
        // Hexadecimal literals are converted to integer literals during compilation
        if self.previous() == '0' && (self.get_current() == 'x' || self.get_current() == 'X') {
            self.advance(); // consumes the "x"
            self.advance_numeric_digit(16); // Consume digit character in base-16
            return self.make_token(HEXADECIMAL_LITERAL);
        }

        // Support for octal integers
        // Octal literals are converted to integer literals during compilation
        if self.previous() == '0' && (self.get_current() == 'o' || self.get_current() == 'O') {
            self.advance(); // consumes the 'o'
            self.advance_numeric_digit(8); // Consume digit character in base-8
            return self.make_token(OCTAL_LITERAL);
        }

        // Support for binary integers
        // Binary literals are converted to integer literals during compilation
        if self.previous() == '0' && (self.get_current() == 'b' || self.get_current() == 'B') {
            self.advance(); // consumes the 'b'
            self.advance_numeric_digit(2); // Consume digit character in base-2
            return self.make_token(BINARY_LITERAL);
        }

        // Checks whether the numeric token started with a dot (to correctly mark it as a float).
        let started_with_dot = self.previous() == '.';
        self.advance_numeric_digit(10); // Consume digit character in base-10

        // Look for a fractional part (only for floats that do not start with a dot).
        if !started_with_dot && self.get_current() == '.' && self.next().is_digit(10) {
            self.advance(); // Consume the ".".
            self.advance_numeric_digit(10); // Consume digit character in base-10
            return self.make_token(FLOAT_LITERAL);
        }

        if started_with_dot {
            return self.make_token(FLOAT_LITERAL);
        } else {
            return self.make_token(INTEGER_LITERAL);
        }
    }

    /// Consumes digit characters of the the given radix base.
    ///
    /// # Arguments
    /// * `radix` – The base of the expected digit.
    pub(self) fn advance_numeric_digit(&mut self, radix: u32) {
        while !self.is_at_end() && self.get_current().is_digit(radix) || (self.get_current() == '_' && self.previous() != '_') {
            self.advance();
        }
    }
}
