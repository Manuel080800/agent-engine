package io.agentengine.webapp.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebappController {
    @GetMapping({"/", "/login"})
    public String index() {
        return "forward:/index.html";
    }
}
