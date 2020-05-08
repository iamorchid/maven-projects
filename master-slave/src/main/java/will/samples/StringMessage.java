package will.samples;

import lombok.AllArgsConstructor;
import lombok.Getter;
import will.test.message.IBizMessage;

@AllArgsConstructor
@Getter
public class StringMessage implements IBizMessage {
    private String content;
}
