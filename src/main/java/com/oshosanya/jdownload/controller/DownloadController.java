package com.oshosanya.jdownload.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class DownloadController {
    @RequestMapping("/")
    public String index()
    {
        return "index.jsp";
    }
}
