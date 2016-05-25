
class B {
b():Int {1};
};
class A inherits B {
b():Int {0};
b2():String {"asdf"};
};

class C inherits A {
b():Int {2};
};
class Main {
    io:IO <- new IO;
    a:Int <- 11;
    b:A;
    c:String <- "asdfasdfasdfasdfasdf";
    d:Bool <- true;
    main():Object {{
        io.out_int(123); 
    }};
};

