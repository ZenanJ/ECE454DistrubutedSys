exception IllegalArgument {
  1: string message;
}

service BcryptService {
 list<string> hashPassword (1: list<string> password, 2: i16 logRounds) throws (1: IllegalArgument e);
 list<bool> checkPassword (1: list<string> password, 2: list<string> hash) throws (1: IllegalArgument e);
 bool BEconnected(1: string host, 2: i32 port) throws (1: IllegalArgument e);
 list<string> hashPasswordFunction(1: list<string> password, 2: i16 logRounds) throws (1: IllegalArgument e);
 list<bool> checkPasswordFunction (1: list<string> password, 2: list<string> hash) throws (1: IllegalArgument e);
}