import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class PahoMqttClientApp {

    public static void main(String appArgs[]) throws Exception {

/*
        // beta 1.0
        String serverUrl = "tcp://10.27.20.52:11883";

        String clientId = "will-dev03";
        String userName = "dev03";
        String passwd = "DWXD/9yhn2Mv4nsBcsUL8h3Rvq4xK/zud1uuxE9On5iRuIZvKvQdV/5nv2M=";
*/
        // beta 2.0
        String serverUrl = "tcp://localhost:11883"; // "tcp://beta-iot-as-mqtt-cn4.eniot.io:11883";

        String clientId = "mqtt-sample-subdev02|securemode=2,signmethod=sha256,timestamp=1574143977891|";
        String userName = "mqtt-sample-subdev02&gvQGUcaT";
        String passwd = "2c7b8002bcd7e675494f3a6fcbf94a4801dc18566e2da9bfe994e6f05a5f80fb";

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setKeepAliveInterval(120);
        options.setUserName(userName);
        options.setPassword(passwd.toCharArray());
        options.setAutomaticReconnect(false); // enable auto reconnect

        MqttClient mqttClient = new MqttClient(serverUrl, clientId);
        mqttClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                System.out.println("connected to server: " + serverUrl);
            }

            @Override
            public void connectionLost(Throwable throwable) {
                System.out.println("lost connection, wait for re-connection");
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) {
                System.out.println("received the following new message >>>");
                System.out.println("  topic: " + topic);
                System.out.println("content: " + new String(mqttMessage.getPayload()));
                System.out.println("    qos: " + mqttMessage.getQos());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                System.out.println("sent a message");
            }
        });
        mqttClient.setTimeToWait(30 * 1000);
        mqttClient.connect(options);

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                System.out.print("> ");

                String input = reader.readLine().trim();
                if (StringUtils.isBlank(input)) {
                    continue;
                }

                if (Objects.equals("exit", input)) {
                    break;
                }

                if (!mqttClient.isConnected()) {
                    System.out.println("not connected, please retry later");
                    continue;
                }

                String[] args = input.split("\\s+");
                switch (args[0]) {
                    case "unsubscribe":
                        if (args.length != 2) {
                            System.out.println("invalid input: " + input);
                            break;
                        }
                        mqttClient.unsubscribe(args[1]);
                        break;
                    case "subscribe":
                        if (args.length < 2 || args.length > 3) {
                            System.out.println("invalid input: " + input);
                            break;
                        }
                        int subQos = MqttQoS.AT_LEAST_ONCE.value();
                        if (args.length == 3) {
                            subQos = Integer.parseInt(args[2]);
                        }

                        mqttClient.subscribe(args[1], subQos);
                        break;
                    case "publish":
                        if (args.length < 3 || args.length > 4) {
                            System.out.println("invalid input: " + input);
                            break;
                        }
                        int pubQos = MqttQoS.AT_LEAST_ONCE.value();
                        if (args.length == 4) {
                            pubQos = Integer.parseInt(args[3]);
                        }
                        mqttClient.publish(args[1], args[2].getBytes(StandardCharsets.UTF_8), pubQos, false);
                        break;
                    default:
                        System.out.println("invalid input: " + input);
                }
            } catch (Throwable e) {
                System.out.println("caught un-expected error: " + e.getMessage());
            }
        }

        mqttClient.disconnect();
        System.out.println("done");
    }
}
