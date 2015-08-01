# Agni Messaging System for Java

Agni, at its core, is a simple system for dropping messages into a black hole and handling them somewhere else.  The somewhere else can be on the same JVM or attached through a network without any changes to the user code.

Here is a very simple example:

    Agni.register(new Object() {
        @Subscribe
        public void handle(MyMessage in) {
            //do something
        }
    });

    Agni.send(new MyMessage(...));

The above block shows the simplicity of registering a handler for a message and then sending a message to that handler.  The send call will return immediately and a threadpool within Agni will be responsible for routing the message and calling the handler.

Want a response?

    Agni.register(new Object() {
        @Subscribe
        public String handle(MyMessage in) {
            //do something
            return "It worked";
        }
    });

    System.out.println(Agni.request(new MyMessage(...).getPayload());

