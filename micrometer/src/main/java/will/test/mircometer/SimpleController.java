package will.test.mircometer;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimpleController {

    @RequestMapping("/getName")
    public String getName(){
        return "prometheus-demo";
    }

    @RequestMapping("/getNameWaiting")
    public String getNameWaiting(){
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "prometheus-demo-waiting";
    }

}
