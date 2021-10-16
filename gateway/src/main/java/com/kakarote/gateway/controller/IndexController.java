package com.kakarote.gateway.controller;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSONObject;
import com.kakarote.core.common.Const;
import com.kakarote.core.common.Result;
import com.kakarote.core.exception.CrmException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.sql.DataSource;
import java.net.URI;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * @author Administrator
 */
@RestController
public class IndexController {

    @Autowired
    private DataSource dataSource;

    @RequestMapping("/")
    public Mono<Void> index(ServerHttpResponse response) {
        return Mono.fromRunnable(() -> {
            response.setStatusCode(HttpStatus.FOUND);
            response.getHeaders().setLocation(URI.create("./index.html"));
        });
    }

    @RequestMapping("/ping")
    public Result ping() {
        return Result.ok();
    }




}
