package com.fgsqw.lanshare.web;

import java.io.IOException;

public interface HttpHandler {
    void handle(Request request, Response response) throws IOException;
}
