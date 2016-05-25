
(*  Example cool program testing as many aspects of the code generator
    as possible.
 *)
class Main {
    io:IO <- new IO;
    x:Object;
    main():Object {{
        x <- 10;
        let x:Object in {
        x <- 10;
        case x of 
            a:IO => io.out_string("matched IO\n"); 
            b:Int => io.out_string("matched Int\n");
            c:Object => io.out_string("matched Object\n");
        esac;
        };
    }};
};

