package be.witspirit.sot;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.api.services.storage.model.Objects;
import com.google.api.services.storage.model.StorageObject;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/")
public class BaseController {

    private static final String APPLICATION_NAME = "sot-guestbook";
    private static final String SERVICE_ACCOUNT_EMAIL = "917597767525-hmqd3lvoep1ppht3jk03usihukkpp12f@developer.gserviceaccount.com";

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static HttpTransport httpTransport;

    private static final String KEYSTORE_LOCATION = "WEB-INF/key.p12";

    private static Storage storage;

    @RequestMapping(value = "/guestbook", method = RequestMethod.GET)
    public String guestbook(ModelMap model, HttpServletRequest request) throws IOException {
        UserService userService = UserServiceFactory.getUserService();
        User currentUser = userService.getCurrentUser();

        if (currentUser != null) {
            model.addAttribute("user", currentUser);

            // Store some data
            DatastoreService service = DatastoreServiceFactory.getDatastoreService();

            Entity greeting = new Entity("Greeting");
            greeting.setProperty("user", currentUser);

            service.put(greeting);

            try {
                setUpStorage();
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
            }

            // Generates the query
            Storage.Objects.List objList = storage.objects().list("sot-gcp-appengine");

            List<StorageObject> items = new ArrayList<>();
            Objects objects;
            do {
                // Performs the actual query
                objects = objList.execute();
                for (StorageObject object : objects.getItems()) {
                    items.add(object);
                }
                // Instruct the query to go to the next page
                objList.setPageToken(objects.getNextPageToken());

            } while (objects.getNextPageToken() != null);

            model.addAttribute("items", items);


        } else {
            // Something funky still here... .jsp gets added somewhere
            return userService.createLoginURL(request.getRequestURI());
        }

        return "guestbook";
    }

    @RequestMapping(value = "/guestbook", method = RequestMethod.POST)
    public String guestbookPost(ModelMap model, HttpServletRequest request) throws IOException {
        return guestbook(model, request);
    }

    private Credential authorize() throws IOException, GeneralSecurityException {
        Set<String> scopes = new HashSet<>();
        scopes.add(StorageScopes.DEVSTORAGE_FULL_CONTROL);
        scopes.add(StorageScopes.DEVSTORAGE_READ_ONLY);
        scopes.add(StorageScopes.DEVSTORAGE_READ_WRITE);

        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(JSON_FACTORY)
                .setServiceAccountId(SERVICE_ACCOUNT_EMAIL)
                .setServiceAccountScopes(scopes)
                .setServiceAccountPrivateKeyFromP12File(new File(KEYSTORE_LOCATION))
                .build();

        return credential;
    }

    private void setUpStorage() throws GeneralSecurityException, IOException {
        httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = authorize();

        // Setup global Storage instance
        storage = new Storage.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

}
