package pl.mwa.client;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import pl.mwa.representative.Representative;
import pl.mwa.representative.RepresentativeService;

@Controller
@RequestMapping("/client")
public class ClientController {
	
	@Autowired
	ClientService clientService;

	@Autowired
	RepresentativeService repService;

	@GetMapping("/add")
	public String add(Model model) {
		model.addAttribute("client", new Client());
		model.addAttribute("representatives", repService.findAllByActiveTrue());
		return "/client/addclient";
	}
	
	@ResponseBody
	@PostMapping("/add")	
	public String add(@Valid Client client, BindingResult	result) {
		if (result.hasErrors()) {
			return "/client/add";
		} else {
			clientService.save(client);
			return "client added";
		}
	}
	



}
