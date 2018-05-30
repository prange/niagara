package org.kantega.niagara.http.server;

import io.undertow.Undertow;

public class Server {

    public static void main(String[] args) {
        Undertow undertow = Undertow.builder().setWorkerThreads(2).build();
    }
}
