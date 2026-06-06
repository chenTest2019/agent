package org.chen;

import org.chen.launch.Launch;


import java.lang.instrument.Instrumentation;

public class App {

    static void main(String[] args) {
        IO.println("main");
    }

    public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
        IO.println("agentmain");
        premain(agentArgs, inst);
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        IO.println("premain");
        try {
            start(agentArgs, inst);
        } catch (Exception e) {
            e.printStackTrace();
            IO.println("start error " + e);
        }

    }

    public static void start(String agentArgs, Instrumentation inst) {
        IO.println("start {}"+ agentArgs);
//        LaunchTest launchTest = new LaunchTest(agentArgs, inst);
//        launchTest.start();
        Launch launch = new Launch(agentArgs, inst);
        launch.start();
    }

}
