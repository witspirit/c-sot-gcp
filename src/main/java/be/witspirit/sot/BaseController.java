package be.witspirit.sot;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/")
public class BaseController {

    @RequestMapping(value = "/guestbook", method = RequestMethod.GET)
    public String guestbook(ModelMap model, HttpServletRequest request) {
        UserService userService = UserServiceFactory.getUserService();
        User currentUser = userService.getCurrentUser();

        if (currentUser != null) {
            model.addAttribute("user", currentUser);
        } else {
            // Something funky still here... .jsp gets added somewhere
            return userService.createLoginURL(request.getRequestURI());
        }

        return "guestbook";
    }

    @RequestMapping(value = "/guestbook", method = RequestMethod.POST)
    public String guestbookPost(ModelMap model, HttpServletRequest request) {
        return guestbook(model, request);
    }

}
