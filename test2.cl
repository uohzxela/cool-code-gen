class A {
i:Int <- 14;
f():Int {i};
};
class B inherits A {
g(): Int {f() + f()};
};
class C inherits B {
h():Int {
let x: A <- new C in {
x.g() + x.f();
}
};
};

class Main {
main():Int {
let c: C <- new C in {c.h(); }
};
};
