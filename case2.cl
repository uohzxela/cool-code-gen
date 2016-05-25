
(*  Example cool program testing as many aspects of the code generator
    as possible.
 *)
class Main {
    io:IO <- new IO;
    x:Object;
    main():Object {{
        x <- 10;
        case x of 
            b:Int => io.out_string("matched Int\n");
            --c:String => io.out_string("matched String\n");
            --c:Object => io.out_string("matched Object\n");
        esac;
    }};
};

