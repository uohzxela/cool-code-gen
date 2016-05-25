
(*  Example cool program testing as many aspects of the code generator
    as possible.
 *)
class Main inherits IO {
    x:IO;
    main():Object {{
        if not isvoid x then 
            out_string("true\n")
        else
            out_string("false\n")
        fi;
    }};
};

