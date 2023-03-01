package com.fgsqw.lanshare.web;

import java.io.IOException;

/**
 * 路径匹配操作
 * @Author: fgsqme
 */
public interface HttpHandler {
    void handle(Request request, Response response) throws IOException;
}
