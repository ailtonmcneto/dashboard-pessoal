package com.vida.hoje;

import com.vida.hoje.HojeDtos.HojeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HojeController {

    private final HojeService service;

    HojeController(HojeService service) {
        this.service = service;
    }

    /** A tela inicial inteira: alertas, tarefas, aulas, provas, eventos, revisões, hábitos, saúde e dinheiro. */
    @GetMapping("/hoje")
    HojeResponse hoje() {
        return service.today();
    }
}
