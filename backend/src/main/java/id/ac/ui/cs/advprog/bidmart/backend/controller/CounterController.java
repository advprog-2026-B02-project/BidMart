package id.ac.ui.cs.advprog.bidmart.backend.controller;

import id.ac.ui.cs.advprog.bidmart.backend.model.PageView;
import id.ac.ui.cs.advprog.bidmart.backend.repository.CounterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/counter")
@CrossOrigin(origins = "*") // biar Next.js nggak kena CORS
public class CounterController {

    @Autowired
    private CounterRepository repo;

    @GetMapping
    public Integer getAndIncrement() {
        PageView view = repo.findById("main_page").orElse(new PageView());
        view.setCount(view.getCount() + 1);
        repo.save(view);
        return view.getCount();
    }
}