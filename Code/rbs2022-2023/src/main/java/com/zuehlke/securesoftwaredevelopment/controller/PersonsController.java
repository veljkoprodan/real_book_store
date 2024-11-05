package com.zuehlke.securesoftwaredevelopment.controller;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.config.SecurityUtil;
import com.zuehlke.securesoftwaredevelopment.domain.Person;
import com.zuehlke.securesoftwaredevelopment.domain.User;
import com.zuehlke.securesoftwaredevelopment.repository.PersonRepository;
import com.zuehlke.securesoftwaredevelopment.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.nio.file.AccessDeniedException;
import java.sql.SQLException;
import java.util.List;

@Controller

public class PersonsController {

    private static final Logger LOG = LoggerFactory.getLogger(PersonsController.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(PersonRepository.class);

    private final PersonRepository personRepository;
    private final UserRepository userRepository;

    public PersonsController(PersonRepository personRepository, UserRepository userRepository) {
        this.personRepository = personRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/persons/{id}")
    @PreAuthorize("hasAuthority('VIEW_PERSON')") // 5. Autorizacija
    public String person(@PathVariable int id, Model model, HttpSession session) {
        model.addAttribute("CSRF_TOKEN", session.getAttribute("CSRF_TOKEN"));  // 4.2. CSRF
        model.addAttribute("person", personRepository.get("" + id));

        User currentUser = SecurityUtil.getCurrentUser();
        LOG.info("User " + currentUser.getId() + " visited profile of person " + id);

        return "person";
    }

    @GetMapping("/myprofile")
    @PreAuthorize("hasAuthority('VIEW_MY_PROFILE')") // 5. Autorizacija
    public String self(Model model, Authentication authentication, HttpSession session) {
        User user = (User) authentication.getPrincipal();
        model.addAttribute("CSRF_TOKEN", session.getAttribute("CSRF_TOKEN"));  // 4.2. CSRF
        model.addAttribute("person", personRepository.get("" + user.getId()));

        LOG.info("User " + user.getId() + " visited their profile");

        return "person";
    }

    @DeleteMapping("/persons/{id}")
    public ResponseEntity<Void> person(@PathVariable int id) {
        personRepository.delete(id);
        userRepository.delete(id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/update-person")
    public String updatePerson(Person person, HttpSession session, @RequestParam("csrfToken") String csrfToken)
    throws AccessDeniedException
    {
        // 5. Autorizacija
        if(SecurityUtil.hasPermission("UPDATE_PERSON") ||
           SecurityUtil.getCurrentUser().getId() == Integer.parseInt(person.getId())
        ){
            // 4.2. CSRF
            String csrf = session.getAttribute("CSRF_TOKEN").toString();
            if (!csrf.equals(csrfToken)) {
                throw new AccessDeniedException("Forbidden");
            }
            personRepository.update(person);

            if(SecurityUtil.getCurrentUser().getId() == Integer.parseInt(person.getId())){
                return "redirect:/myprofile";
            }
            else{
                return "redirect:/persons/" + person.getId();
            }
        }
        else throw new AccessDeniedException("Forbidden");
    }

    @GetMapping("/persons")
    @PreAuthorize("hasAuthority('VIEW_PERSONS_LIST')") // 5. Autorizacija
    public String persons(Model model) {
        model.addAttribute("persons", personRepository.getAll());
        return "persons";
    }

    @GetMapping(value = "/persons/search", produces = "application/json")
    @ResponseBody
    public List<Person> searchPersons(@RequestParam String searchTerm) throws SQLException {
        return personRepository.search(searchTerm);
    }
}
