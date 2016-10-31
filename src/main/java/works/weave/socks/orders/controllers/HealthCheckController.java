package works.weave.socks.orders.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import works.weave.socks.orders.entities.HealthCheck;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class HealthCheckController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(method = RequestMethod.GET, path = "/health")
    public
    @ResponseBody
    Map<String, List<HealthCheck>> getHealth() {
      Map<String, List<HealthCheck>> map = new HashMap<String, List<HealthCheck>>();
      List<HealthCheck> healthChecks = new ArrayList<HealthCheck>();
      Date dateNow = Calendar.getInstance().getTime();

      HealthCheck app = new HealthCheck("orders", "OK", dateNow);
      HealthCheck database = new HealthCheck("orders-db", "OK", dateNow);

      try {
         mongoTemplate.executeCommand("{ buildInfo: 1 }");
      } catch (Exception e) {
         database.setStatus("err");
      }

      healthChecks.add(app);
      healthChecks.add(database);

      map.put("health", healthChecks);
      return map;
    }
}
