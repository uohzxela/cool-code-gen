
(*  Example cool program testing as many aspects of the code generator
    as possible.
 *)
class Main {
    io:IO <- new IO;
    --x:Parent;
    x:Int;
    main():Object {{
        --x <- new Child;
        x <- 10;
        case x of 
            c:Object => io.out_string("matched Object\n");
            c:String => {io.out_string("matched String\n"); c;};
            c:Child => io.out_string("matched Child\n");
            c:Parent => io.out_string("matched Parent\n");
            b:Int => io.out_string("matched Int\n");
        esac;
    }};
};
class GrandChild2 inherits Child {};
class Parent {};
class GrandChild inherits Child {};
class Child inherits Parent {};

